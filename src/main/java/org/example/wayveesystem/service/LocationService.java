package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.dto.response.LocationPageResponse;
import org.example.wayveesystem.dto.response.NominatimReverseResponse;

public interface LocationService {
    LocationPageResponse searchNearbyLocations(LocationFilterRequest request);
    LocationResponse getPlaceByOsmId(Long osmId);
    NominatimReverseResponse reverseGeocode(Double lat, Double lng);
}
