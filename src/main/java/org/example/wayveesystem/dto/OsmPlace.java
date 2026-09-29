package org.example.wayveesystem.dto;

import org.example.wayveesystem.common.enums.ImageSource;

import java.io.Serializable;
import java.util.Map;

/**
 * In-memory POJO representing a Point of Interest from OpenStreetMap.
 * NOT a JPA entity — never persisted to the database.
 * Used for cache storage and API responses.
 */
public record OsmPlace(
        Long osmId,
        String name,
        String categoryCode,
        String address,
        Double latitude,
        Double longitude,
        String imageUrl,
        ImageSource imageSource,
        String openingHours,
        String phone,
        String website,
        Map<String, String> tags
) implements Serializable {
}
