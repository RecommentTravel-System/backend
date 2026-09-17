package org.example.wayveesystem.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Cache key for grid-based POI lookups.
 * Coordinates are snapped to a grid to increase cache hit rate.
 */
public record GridCacheKey(
        double gridLat,
        double gridLng,
        List<String> categories,
        List<String> cuisines,
        int radiusMeters
) implements Serializable {

    /**
     * Snaps a coordinate to the nearest grid point based on the given precision.
     * For example, with precision 0.005 (~500m), lat 10.7731 snaps to 10.775.
     */
    public static double snapToGrid(double coordinate, double precision) {
        return Math.round(coordinate / precision) * precision;
    }
}
