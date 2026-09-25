package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.TripRequest;
import org.example.wayveesystem.dto.response.TripResponse;

import java.util.List;

public interface TripService {
    TripResponse createTrip(TripRequest request);
    List<TripResponse> getMyTrips();
    TripResponse getTripById(Long tripId);
    TripResponse updateTrip(Long tripId, TripRequest request);
    TripResponse confirmItinerary(Long tripId);
    void deleteTrip(Long tripId);
}
