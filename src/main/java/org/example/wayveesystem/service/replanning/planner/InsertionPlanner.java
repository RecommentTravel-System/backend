package org.example.wayveesystem.service.replanning.planner;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationPriority;
import org.example.wayveesystem.common.enums.ReplanAction;
import org.example.wayveesystem.common.enums.ReplanSeverity;
import org.example.wayveesystem.service.replanning.constraint.ConstraintValidationResult;
import org.example.wayveesystem.service.replanning.constraint.ReplanConstraintChecker;
import org.example.wayveesystem.service.replanning.constraint.ScheduleTimeEvaluator;
import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InsertionPlanner {

    List<ReplanConstraintChecker> constraintCheckers;
    ScheduleTimeEvaluator scheduleTimeEvaluator;
    LocalSearchOptimizer localSearchOptimizer;

    public InsertionPlanResult planInsertions(
            List<DaySchedule> targetDays,
            List<ReplanLocationItem> unvisitedLocations,
            DistanceMatrixCalculator distanceCalculator) {

        if (targetDays == null) {
            targetDays = new ArrayList<>();
        }

        // Deep copy candidate schedules so we don't mutate external references
        List<DaySchedule> workingDays = new ArrayList<>();
        for (DaySchedule ds : targetDays) {
            workingDays.add(ds.deepCopy());
        }

        List<UnplaceableLocationItem> unplaceables = new ArrayList<>();
        Set<Integer> modifiedDays = new HashSet<>();
        ReplanSeverity overallSeverity = ReplanSeverity.NORMAL;

        if (unvisitedLocations == null || unvisitedLocations.isEmpty()) {
            return InsertionPlanResult.builder()
                    .schedules(workingDays)
                    .unplaceableLocations(unplaceables)
                    .modifiedDays(modifiedDays)
                    .severity(overallSeverity)
                    .build();
        }

        // 1. Sort U by most constrained first
        List<ReplanLocationItem> sortedU = new ArrayList<>(unvisitedLocations);
        sortedU.sort((a, b) -> {
            // (1) fixed_day constraint
            boolean aFixed = a.getFixedDay() != null;
            boolean bFixed = b.getFixedDay() != null;
            if (aFixed != bFixed) return aFixed ? -1 : 1;

            // (2) narrowest opening hours window
            long aWindow = getOpeningWindowMinutes(a);
            long bWindow = getOpeningWindowMinutes(b);
            if (aWindow != bWindow) return Long.compare(aWindow, bWindow);

            // (3) longest stay duration first
            int aStay = a.getStayDurationMinutes() != null ? a.getStayDurationMinutes() : 60;
            int bStay = b.getStayDurationMinutes() != null ? b.getStayDurationMinutes() : 60;
            if (aStay != bStay) return Integer.compare(bStay, aStay);

            // (4) MUST_GO before NICE_TO_HAVE
            int aPrio = a.getPriority() == LocationPriority.MUST_GO ? 0 : 1;
            int bPrio = b.getPriority() == LocationPriority.MUST_GO ? 0 : 1;
            return Integer.compare(aPrio, bPrio);
        });

        // 2. Iterate through sorted U and apply Cheapest Insertion
        for (ReplanLocationItem candidate : sortedU) {
            DaySchedule bestDay = null;
            int bestInsertIndex = -1;
            double minDeltaCost = Double.MAX_VALUE;
            ScheduleEvaluationResult bestEvalResult = null;

            for (DaySchedule daySchedule : workingDays) {
                // If candidate has fixedDay and doesn't match this day, skip
                if (candidate.getFixedDay() != null && !candidate.getFixedDay().equals(daySchedule.getDay())) {
                    continue;
                }

                ScheduleEvaluationResult baseEval = scheduleTimeEvaluator.evaluate(daySchedule, distanceCalculator);
                double baseDist = baseEval.isFeasible() ? baseEval.getTotalDistanceKm() : 0.0;

                int locCount = daySchedule.getLocations().size();
                for (int idx = 0; idx <= locCount; idx++) {
                    boolean allPassed = true;
                    for (ReplanConstraintChecker checker : constraintCheckers) {
                        ConstraintValidationResult res = checker.validate(daySchedule, candidate, idx, distanceCalculator);
                        if (!res.isValid()) {
                            allPassed = false;
                            break;
                        }
                    }

                    if (allPassed) {
                        // Candidate passed constraints, evaluate delta cost
                        DaySchedule simDay = daySchedule.deepCopy();
                        simDay.getLocations().add(idx, ReplanLocationItem.copyOf(candidate));
                        ScheduleEvaluationResult newEval = scheduleTimeEvaluator.evaluate(simDay, distanceCalculator);

                        if (newEval.isFeasible()) {
                            double deltaCost = newEval.getTotalDistanceKm() - baseDist;
                            if (deltaCost < minDeltaCost) {
                                minDeltaCost = deltaCost;
                                bestDay = daySchedule;
                                bestInsertIndex = idx;
                                bestEvalResult = newEval;
                            }
                        }
                    }
                }
            }

            if (bestDay != null && bestInsertIndex >= 0) {
                // Insert candidate into best day
                bestDay.getLocations().add(bestInsertIndex, ReplanLocationItem.copyOf(candidate));
                if (bestEvalResult != null && bestEvalResult.getEvaluatedLocations() != null) {
                    bestDay.setLocations(bestEvalResult.getEvaluatedLocations());
                }
                modifiedDays.add(bestDay.getDay());
            } else {
                // Candidate could not be placed directly
                boolean bumped = false;
                if (candidate.getPriority() == LocationPriority.MUST_GO) {
                    // Try bumping an unvisited NICE_TO_HAVE from one of the target days
                    bumpSearch:
                    for (DaySchedule daySchedule : workingDays) {
                        if (candidate.getFixedDay() != null && !candidate.getFixedDay().equals(daySchedule.getDay())) {
                            continue;
                        }

                        for (int i = 0; i < daySchedule.getLocations().size(); i++) {
                            ReplanLocationItem victim = daySchedule.getLocations().get(i);
                            if (victim.getPriority() == LocationPriority.NICE_TO_HAVE && victim.getFixedDay() == null) {
                                // Try removing victim and inserting candidate
                                DaySchedule testDay = daySchedule.deepCopy();
                                testDay.getLocations().remove(i);

                                for (int k = 0; k <= testDay.getLocations().size(); k++) {
                                    boolean valid = true;
                                    for (ReplanConstraintChecker checker : constraintCheckers) {
                                        if (!checker.validate(testDay, candidate, k, distanceCalculator).isValid()) {
                                            valid = false;
                                            break;
                                        }
                                    }
                                    if (valid) {
                                        DaySchedule sim = testDay.deepCopy();
                                        sim.getLocations().add(k, ReplanLocationItem.copyOf(candidate));
                                        ScheduleEvaluationResult eval = scheduleTimeEvaluator.evaluate(sim, distanceCalculator);
                                        if (eval.isFeasible()) {
                                            // Bump succeeds!
                                            daySchedule.getLocations().remove(i);
                                            daySchedule.getLocations().add(k, ReplanLocationItem.copyOf(candidate));
                                            daySchedule.setLocations(eval.getEvaluatedLocations());
                                            modifiedDays.add(daySchedule.getDay());

                                            // Record bumped victim into unplaceable
                                            unplaceables.add(UnplaceableLocationItem.builder()
                                                    .tripLocationId(victim.getTripLocationId())
                                                    .osmId(victim.getOsmId())
                                                    .placeName(victim.getPlaceName())
                                                    .priority(victim.getPriority())
                                                    .originalDay(daySchedule.getDay())
                                                    .reason("BUMPED_BY_MUST_GO_LOCATION: " + candidate.getPlaceName())
                                                    .severity(ReplanSeverity.HIGH)
                                                    .suggestedActions(List.of(ReplanAction.SKIP, ReplanAction.ADD_EXTRA_DAY))
                                                    .build());
                                            overallSeverity = ReplanSeverity.HIGH;
                                            bumped = true;
                                            break bumpSearch;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!bumped) {
                    ReplanSeverity itemSeverity = candidate.getPriority() == LocationPriority.MUST_GO
                            ? ReplanSeverity.HIGH
                            : ReplanSeverity.NORMAL;
                    if (itemSeverity == ReplanSeverity.HIGH) {
                        overallSeverity = ReplanSeverity.HIGH;
                    }

                    unplaceables.add(UnplaceableLocationItem.builder()
                            .tripLocationId(candidate.getTripLocationId())
                            .osmId(candidate.getOsmId())
                            .placeName(candidate.getPlaceName())
                            .priority(candidate.getPriority())
                            .originalDay(candidate.getPlannedDay())
                            .reason("NO_FEASIBLE_TIME_WINDOW_OR_CAPACITY")
                            .severity(itemSeverity)
                            .suggestedActions(List.of(ReplanAction.SKIP, ReplanAction.ADD_EXTRA_DAY))
                            .build());
                }
            }
        }

        // 3. Run LocalSearchOptimizer on modified days
        for (DaySchedule day : workingDays) {
            if (modifiedDays.contains(day.getDay())) {
                DaySchedule optimized = localSearchOptimizer.optimizeDay(day, distanceCalculator);
                day.setLocations(optimized.getLocations());
            }
        }

        return InsertionPlanResult.builder()
                .schedules(workingDays)
                .unplaceableLocations(unplaceables)
                .modifiedDays(modifiedDays)
                .severity(overallSeverity)
                .build();
    }

    private long getOpeningWindowMinutes(ReplanLocationItem item) {
        if (item.getOpenTime() != null && item.getCloseTime() != null) {
            long mins = Duration.between(item.getOpenTime(), item.getCloseTime()).toMinutes();
            return mins > 0 ? mins : 1440 + mins;
        }
        return 1440; // Default full day window
    }
}
