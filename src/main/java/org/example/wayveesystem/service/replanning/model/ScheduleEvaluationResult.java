package org.example.wayveesystem.service.replanning.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduleEvaluationResult {
    boolean feasible;
    String rejectionReason;
    double totalDistanceKm;
    int totalTravelMinutes;
    int totalStayMinutes;
    int totalWaitingMinutes;
    LocalTime finalDepartureTime;
    @Builder.Default
    List<ReplanLocationItem> evaluatedLocations = new ArrayList<>();
}
