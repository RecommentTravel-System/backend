package org.example.wayveesystem.service.replanning.constraint;

import org.example.wayveesystem.service.replanning.distance.DistanceMatrixCalculator;
import org.example.wayveesystem.service.replanning.model.DaySchedule;
import org.example.wayveesystem.service.replanning.model.ReplanLocationItem;

public interface ReplanConstraintChecker {
    /**
     * Validates whether inserting candidate into schedule at the given index satisfies this constraint.
     */
    ConstraintValidationResult validate(DaySchedule schedule, ReplanLocationItem candidate, int insertIndex, DistanceMatrixCalculator distanceCalculator);
}
