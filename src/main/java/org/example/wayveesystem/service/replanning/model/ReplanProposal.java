package org.example.wayveesystem.service.replanning.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.dto.response.MetricsDiffDto;
import org.example.wayveesystem.dto.response.MovedLocationDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplanProposal {
    String proposalId;
    Long tripId;
    Long tripVersion;
    Integer currentDay;
    LocalDateTime createdAt;
    List<DaySchedule> newSchedules;
    List<UnplaceableLocationItem> unplaceableLocations;
    List<MovedLocationDto> movedLocations;
    MetricsDiffDto metricsDiff;
}
