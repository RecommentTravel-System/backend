package org.example.wayveesystem.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;

import java.time.LocalDateTime;

/**
 * Compact catalog table for storing raw/imported POIs from OSM and Overture.
 * Used exclusively for search, exploration, and itinerary recommendations.
 */
@Entity
@Table(name = "poi_catalog",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_poi_catalog_source_source_id", columnNames = {"source", "source_id"})
        },
        indexes = {
                @Index(name = "idx_poi_catalog_lat_lon", columnList = "lat, lon"),
                @Index(name = "idx_poi_catalog_category_id", columnList = "category_id"),
                @Index(name = "idx_poi_catalog_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoiCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    LocationSource source;

    @Column(name = "source_id", nullable = false, length = 128)
    String sourceId;

    @Column(name = "name", nullable = false, length = 255)
    String name;

    @Column(name = "category_id")
    Long categoryId;

    @Column(name = "address", length = 500)
    String address;

    @Column(name = "lat", nullable = false)
    Float lat;

    @Column(name = "lon", nullable = false)
    Float lon;

    @Column(name = "confidence")
    Float confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    PoiStatus status;

    @Column(name = "imported_at", nullable = false)
    LocalDateTime importedAt;
}
