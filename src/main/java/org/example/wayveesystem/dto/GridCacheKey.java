package org.example.wayveesystem.dto;

import java.io.Serializable;

/**
 * Cache key for grid-based POI lookups.
 * Coordinates are snapped to a grid, independent of categories, cuisines, or user filters.
 * Caches all POIs within a fixed maximum radius (e.g., 10,000 meters / 10km).
 */
public record GridCacheKey(
        double gridLat,
        double gridLng,
        int maxRadiusMeters
) implements Serializable {

    public GridCacheKey(double gridLat, double gridLng) {
        this(gridLat, gridLng, 3000); // Reduced to 3km max grid radius to avoid Overpass timeouts
    }

    /**
     * Snaps a coordinate to the nearest grid point based on the given precision.
     * For example, with precision 0.005 (~500m), lat 10.7731 snaps to 10.775.
     */
    public static double snapToGrid(double coordinate, double precision) {
        return Math.round(coordinate / precision) * precision;
    }
}

