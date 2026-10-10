package org.example.wayveesystem.service.replanning.constraint;

import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.DaySchedule;
import org.example.wayveesystem.service.replanning.model.ReplanLocationItem;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class FixedDayChecker implements ReplanConstraintChecker {

    @Override
    public ConstraintValidationResult validate(DaySchedule schedule, ReplanLocationItem candidate, int insertIndex, DistanceMatrixCalculator distanceCalculator) {
        if (candidate == null || schedule == null) {
            return ConstraintValidationResult.fail("Candidate or schedule is null");
        }

        if (candidate.getFixedDay() != null) {
            if (!candidate.getFixedDay().equals(schedule.getDay())) {
                return ConstraintValidationResult.fail(String.format(
                        "Location '%s' is fixed to day %d and cannot be scheduled on day %d",
                        candidate.getPlaceName(), candidate.getFixedDay(), schedule.getDay()
                ));
            }
        }

        return ConstraintValidationResult.success();
    }
}
