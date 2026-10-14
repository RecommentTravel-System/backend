package org.example.wayveesystem.service.replanning.planner;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.service.replanning.constraint.ScheduleTimeEvaluator;
import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.DaySchedule;
import org.example.wayveesystem.service.replanning.model.ReplanLocationItem;
import org.example.wayveesystem.service.replanning.model.ScheduleEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LocalSearchOptimizer {

    ScheduleTimeEvaluator scheduleTimeEvaluator;

    /**
     * Optimizes route order for a single day using 2-opt and Or-opt, strictly enforcing time feasibility.
     */
    public DaySchedule optimizeDay(DaySchedule schedule, DistanceMatrixCalculator distanceCalculator) {
        if (schedule == null || schedule.getLocations() == null || schedule.getLocations().size() < 3) {
            return schedule;
        }

        DaySchedule currentBestSchedule = schedule.deepCopy();
        ScheduleEvaluationResult bestEval = scheduleTimeEvaluator.evaluate(currentBestSchedule, distanceCalculator);
        if (!bestEval.isFeasible()) {
            return schedule;
        }

        double bestDist = bestEval.getTotalDistanceKm();
        boolean improved = true;
        int maxRounds = 20;
        int rounds = 0;

        while (improved && rounds < maxRounds) {
            improved = false;
            rounds++;

            // 1. 2-opt: Reverse sub-segments [i, j]
            int n = currentBestSchedule.getLocations().size();
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    DaySchedule candidateSchedule = currentBestSchedule.deepCopy();
                    List<ReplanLocationItem> locs = candidateSchedule.getLocations();
                    // Reverse sublist from i to j
                    Collections.reverse(locs.subList(i, j + 1));

                    ScheduleEvaluationResult candidateEval = scheduleTimeEvaluator.evaluate(candidateSchedule, distanceCalculator);
                    if (candidateEval.isFeasible() && candidateEval.getTotalDistanceKm() < bestDist - 0.001) {
                        bestDist = candidateEval.getTotalDistanceKm();
                        currentBestSchedule = candidateSchedule;
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }

            if (improved) continue;

            // 2. Or-opt: Relocate chains of length 1, 2, or 3
            for (int chainLen = 1; chainLen <= Math.min(3, n - 2); chainLen++) {
                for (int i = 0; i <= n - chainLen; i++) {
                    for (int j = 0; j <= n - chainLen; j++) {
                        if (i == j) continue;

                        DaySchedule candidateSchedule = currentBestSchedule.deepCopy();
                        List<ReplanLocationItem> locs = candidateSchedule.getLocations();

                        // Extract chain
                        List<ReplanLocationItem> chain = new ArrayList<>();
                        for (int k = 0; k < chainLen; k++) {
                            chain.add(locs.remove(i));
                        }

                        // Re-insert chain at j
                        int targetIndex = j > i ? j - chainLen + 1 : j;
                        targetIndex = Math.max(0, Math.min(targetIndex, locs.size()));
                        locs.addAll(targetIndex, chain);

                        ScheduleEvaluationResult candidateEval = scheduleTimeEvaluator.evaluate(candidateSchedule, distanceCalculator);
                        if (candidateEval.isFeasible() && candidateEval.getTotalDistanceKm() < bestDist - 0.001) {
                            bestDist = candidateEval.getTotalDistanceKm();
                            currentBestSchedule = candidateSchedule;
                            improved = true;
                            break;
                        }
                    }
                    if (improved) break;
                }
                if (improved) break;
            }
        }

        // Apply final evaluated visit orders and estimated arrival times
        ScheduleEvaluationResult finalEval = scheduleTimeEvaluator.evaluate(currentBestSchedule, distanceCalculator);
        if (finalEval.isFeasible() && finalEval.getEvaluatedLocations() != null) {
            currentBestSchedule.setLocations(finalEval.getEvaluatedLocations());
        }

        return currentBestSchedule;
    }
}
