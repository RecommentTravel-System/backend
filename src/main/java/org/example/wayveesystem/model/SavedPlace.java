package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity for places that users have interacted with (favorited, reviewed, added to trip).
 * Only created when a business action occurs — NOT for every POI from OSM.
 */
@Entity
@Table(name = "saved_places", indexes = {
        @Index(name = "idx_saved_place_osm_id", columnList = "osm_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SavedPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saved_place_id")
    Long savedPlaceId;

    @Column(name = "osm_id", unique = true, nullable = false)
    Long osmId;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "category_code")
    String categoryCode;

    @Column(name = "address")
    String address;

    @Column(name = "latitude", nullable = false)
    Double latitude;

    @Column(name = "longitude", nullable = false)
    Double longitude;

    @Column(name = "image_url")
    String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
