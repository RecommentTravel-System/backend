package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    Long routeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    Subscription subscription;

    @Column(name = "start_location", columnDefinition = "TEXT")
    String startLocation;

    @Column(name = "end_location", columnDefinition = "TEXT")
    String endLocation;

    @Column(name = "route_data", columnDefinition = "TEXT")
    String routeData;

    @Column(name = "distance")
    Double distance;

    @Column(name = "estimated_time")
    Integer estimatedTime;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
