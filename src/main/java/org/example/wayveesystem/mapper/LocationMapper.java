package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.model.Location;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationResponse toResponse(Location location, double userLat, double userLng) {
        if (location == null) {
            return null;
        }

        double distance = calculateDistanceMeters(userLat, userLng, location.getLatitude(), location.getLongitude());

        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .sourceOsmId(location.getSourceOsmId())
                .name(location.getName())
                .categoryCode(location.getCategoryCode())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .imageUrl(location.getImageUrl())
                .rating(location.getRating() != null ? location.getRating() : 0.0)
                .reviewCount(location.getReviewCount() != null ? location.getReviewCount() : 0)
                .distanceMeters(Math.round(distance * 100.0) / 100.0)
                .build();
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
