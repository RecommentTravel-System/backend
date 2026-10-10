package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.ReplanSeverity;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplanProposalResponse {
    String proposalId;
    Long tripId;
    Long tripVersion;
    Integer currentDay;
    ReplanSeverity severity;
    List<MovedLocationDto> movedLocations;
    List<UnplaceableLocationDto> unplaceableLocations;
    List<DayScheduleDto> newSchedule;
    MetricsDiffDto metricsDiff;
}
