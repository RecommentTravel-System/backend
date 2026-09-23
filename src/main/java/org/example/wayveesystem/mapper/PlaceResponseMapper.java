package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.model.SavedPlace;
import org.springframework.stereotype.Component;

@Component
public class PlaceResponseMapper {

    /**
     * Maps an in-memory OsmPlace to a LocationResponse DTO.
     * Used for search results from Overpass/cache.
     */
    public LocationResponse toResponse(OsmPlace place, double userLat, double userLng) {
        if (place == null) {
            return null;
        }

        double distance = calculateDistanceMeters(userLat, userLng, place.latitude(), place.longitude());

        return LocationResponse.builder()
                .osmId(place.osmId())
                .name(place.name())
                .categoryCode(place.categoryCode())
                .address(place.address())
                .latitude(place.latitude())
                .longitude(place.longitude())
                .imageUrl(place.imageUrl())
                .imageSource(place.imageSource() == null ? null : place.imageSource().name())
                .openingHours(place.openingHours())
                .phone(place.phone())
                .website(place.website())
                .distanceMeters(Math.round(distance * 100.0) / 100.0)
                .build();
    }

    /**
     * Maps a SavedPlace (DB entity) to a LocationResponse DTO.
     * Used for favorites/reviews/trip locations.
     */
    public LocationResponse toResponse(SavedPlace saved, double userLat, double userLng) {
        if (saved == null) {
            return null;
        }

        double distance = calculateDistanceMeters(userLat, userLng, saved.getLatitude(), saved.getLongitude());

        return LocationResponse.builder()
                .osmId(saved.getOsmId())
                .name(saved.getName())
                .categoryCode(saved.getCategoryCode())
                .address(saved.getAddress())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .imageUrl(saved.getImageUrl())
                .imageSource(saved.getImageSource())
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
