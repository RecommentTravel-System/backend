package org.example.wayveesystem.service.replanning.constraint;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.DaySchedule;
import org.example.wayveesystem.service.replanning.model.ReplanLocationItem;
import org.example.wayveesystem.service.replanning.model.ScheduleEvaluationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DayTimeBudgetChecker implements ReplanConstraintChecker {

    ScheduleTimeEvaluator scheduleTimeEvaluator;

    @Override
    public ConstraintValidationResult validate(DaySchedule schedule, ReplanLocationItem candidate, int insertIndex, DistanceMatrixCalculator distanceCalculator) {
        if (candidate == null || schedule == null) {
            return ConstraintValidationResult.fail("Candidate or schedule is null");
        }

        List<ReplanLocationItem> simLocations = new ArrayList<>(schedule.getLocations());
        int safeIndex = Math.max(0, Math.min(insertIndex, simLocations.size()));
        simLocations.add(safeIndex, candidate);

        DaySchedule simSchedule = DaySchedule.builder()
                .day(schedule.getDay())
                .date(schedule.getDate())
                .startTime(schedule.getStartTime())
                .maxEndTime(schedule.getMaxEndTime())
                .startLatitude(schedule.getStartLatitude())
                .startLongitude(schedule.getStartLongitude())
                .locations(simLocations)
                .build();

        ScheduleEvaluationResult result = scheduleTimeEvaluator.evaluate(simSchedule, distanceCalculator);
        if (!result.isFeasible()) {
            return ConstraintValidationResult.fail(result.getRejectionReason());
        }

        return ConstraintValidationResult.success();
    }
}
