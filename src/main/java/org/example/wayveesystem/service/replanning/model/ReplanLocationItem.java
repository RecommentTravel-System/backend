package org.example.wayveesystem.service.replanning.model;

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
public class ReplanLocationItem {
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

    public static ReplanLocationItem copyOf(ReplanLocationItem item) {
        if (item == null) return null;
        return ReplanLocationItem.builder()
                .tripLocationId(item.getTripLocationId())
                .osmId(item.getOsmId())
                .placeName(item.getPlaceName())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .visitOrder(item.getVisitOrder())
                .plannedDay(item.getPlannedDay())
                .status(item.getStatus())
                .visitedAt(item.getVisitedAt())
                .priority(item.getPriority())
                .fixedDay(item.getFixedDay())
                .stayDurationMinutes(item.getStayDurationMinutes() != null ? item.getStayDurationMinutes() : 60)
                .openTime(item.getOpenTime())
                .closeTime(item.getCloseTime())
                .estimatedArrivalTime(item.getEstimatedArrivalTime())
                .notes(item.getNotes())
                .build();
    }
}
