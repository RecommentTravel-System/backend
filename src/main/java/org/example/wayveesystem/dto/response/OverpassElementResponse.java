package org.example.wayveesystem.dto.response;

import java.util.Map;

public record OverpassElementResponse(
        long id,
        String type,
        Double lat,
        Double lon,
        Center center,
        Map<String, String> tags
) {
    public record Center(double lat, double lon) {}

    public double getLat() {
        if (lat != null) return lat;
        if (center != null) return center.lat();
        return 0.0;
    }

    public double getLon() {
        if (lon != null) return lon;
        if (center != null) return center.lon();
        return 0.0;
    }
}
