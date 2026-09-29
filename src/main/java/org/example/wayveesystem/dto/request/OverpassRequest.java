package org.example.wayveesystem.dto.request;

import java.util.List;

public record OverpassRequest(
        double lat,
        double lng,
        int radiusMeters,
        List<String> osmFilters
) {
    public static OverpassRequest of(double lat, double lng, int radiusMeters, List<String> osmFilters) {
        return new OverpassRequest(lat, lng, radiusMeters, osmFilters);
    }
}