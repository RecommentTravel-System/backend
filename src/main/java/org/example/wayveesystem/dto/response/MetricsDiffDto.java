package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MetricsDiffDto {
    Double oldTotalDistanceKm;
    Double newTotalDistanceKm;
    Double distanceDeltaKm;
    Integer oldTotalTravelMinutes;
    Integer newTotalTravelMinutes;
    Integer travelMinutesDelta;
}
