package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.LocationStatusUpdateRequest;
import org.example.wayveesystem.dto.request.ReplanConfirmRequest;
import org.example.wayveesystem.dto.request.ReplanPreviewRequest;
import org.example.wayveesystem.dto.response.ReplanProposalResponse;
import org.example.wayveesystem.dto.response.TripLocationResponse;
import org.example.wayveesystem.dto.response.TripResponse;

public interface TripReplanningService {
    ReplanProposalResponse previewReplan(Long tripId, ReplanPreviewRequest request);
    TripResponse confirmReplan(Long tripId, ReplanConfirmRequest request);
    TripLocationResponse updateLocationStatus(Long tripId, Long locationId, LocationStatusUpdateRequest request);
}
