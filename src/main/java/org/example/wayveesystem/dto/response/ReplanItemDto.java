package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationPriority;
import org.example.wayveesystem.common.enums.TripLocationStatus;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplanItemDto {
    Long tripLocationId;
    Long osmId;
    String placeName;
    Double latitude;
    Double longitude;
    Integer visitOrder;
    Integer plannedDay;
    TripLocationStatus status;
    LocalDateTime visitedAt;
    LocationPriority priority;
    Integer fixedDay;
    Integer stayDurationMinutes;
    LocalTime openTime;
    LocalTime closeTime;
    LocalTime estimatedArrivalTime;
    String notes;
}
