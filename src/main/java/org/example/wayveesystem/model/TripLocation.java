package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationPriority;
import org.example.wayveesystem.common.enums.TripLocationStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "trip_locations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trip_location_id")
    Long tripLocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    Trip trip;

    @Column(name = "osm_id", nullable = false)
    Long osmId;

    @Column(name = "place_name")
    String placeName;

    @Column(name = "latitude")
    Double latitude;

    @Column(name = "longitude")
    Double longitude;

    @Column(name = "visit_order")
    Integer visitOrder;

    @Column(name = "planned_day")
    Integer plannedDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    @Builder.Default
    TripLocationStatus status = TripLocationStatus.PLANNED;

    @Column(name = "visited_at")
    LocalDateTime visitedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 32)
    @Builder.Default
    LocationPriority priority = LocationPriority.NICE_TO_HAVE;

    @Column(name = "fixed_day")
    Integer fixedDay;

    @Builder.Default
    @Column(name = "stay_duration_minutes")
    Integer stayDurationMinutes = 60;

    @Column(name = "open_time")
    LocalTime openTime;

    @Column(name = "close_time")
    LocalTime closeTime;

    @Column(name = "estimated_arrival_time")
    LocalTime estimatedArrivalTime;

    @Column(name = "notes")
    String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
