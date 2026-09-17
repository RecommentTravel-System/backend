package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LocationFilterRequest(
        @NotNull(message = "Latitude is required")
        Double lat,

        @NotNull(message = "Longitude is required")
        Double lng,

        Integer radiusMeters,
        List<String> categories,
        List<String> cuisines,
        String keyword,
        Double minRating
) {
    public LocationFilterRequest {
        if (radiusMeters == null || radiusMeters <= 0) {
            radiusMeters = 1000;
        } else if (radiusMeters > 10000) {
            radiusMeters = 10000;
        }
    }
}
