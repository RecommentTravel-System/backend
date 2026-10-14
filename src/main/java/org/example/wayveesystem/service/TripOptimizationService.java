package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.ItineraryOptimizeRequest;
import org.example.wayveesystem.dto.response.ItineraryOptimizeResponse;

public interface TripOptimizationService {
    ItineraryOptimizeResponse optimizeItinerary(ItineraryOptimizeRequest request);
}
