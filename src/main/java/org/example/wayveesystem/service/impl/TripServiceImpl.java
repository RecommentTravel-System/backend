package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.TripRequest;
import org.example.wayveesystem.dto.response.TripResponse;
import org.example.wayveesystem.model.Trip;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.TripRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.TripService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripServiceImpl implements TripService {

    TripRepository tripRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public TripResponse createTrip(TripRequest request) {
        Trip trip = Trip.builder()
                .user(getCurrentUser())
                .tripName(request.getTripName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .budget(request.getBudget())
                .status(request.getStatus())
                .itineraryArranged(Boolean.TRUE.equals(request.getItineraryArranged()))
                .build();
        return toResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getMyTrips() {
        return tripRepository.findByUserAndDeletedFalseOrderByCreatedAtDesc(getCurrentUser()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponse getTripById(Long tripId) {
        return toResponse(getActiveTrip(tripId));
    }

    @Override
    @Transactional
    public TripResponse updateTrip(Long tripId, TripRequest request) {
        Trip trip = getActiveTrip(tripId);
        trip.setTripName(request.getTripName().trim());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        trip.setBudget(request.getBudget());
        trip.setStatus(request.getStatus());
        if (request.getItineraryArranged() != null) {
            trip.setItineraryArranged(request.getItineraryArranged());
        }
        return toResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional
    public TripResponse confirmItinerary(Long tripId) {
        Trip trip = getActiveTrip(tripId);
        trip.setItineraryArranged(true);
        return toResponse(tripRepository.save(trip));
    }

    @Override
    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = getActiveTrip(tripId);
        trip.setDeleted(true);
        trip.setDeletedAt(LocalDateTime.now());
        tripRepository.save(trip);
    }

    private User getCurrentUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Trip getActiveTrip(Long tripId) {
        return tripRepository.findByTripIdAndUserAndDeletedFalse(tripId, getCurrentUser())
                .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND));
    }

    private TripResponse toResponse(Trip trip) {
        return TripResponse.builder()
                .tripId(trip.getTripId())
                .tripName(trip.getTripName())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .budget(trip.getBudget())
                .status(trip.getStatus())
                .itineraryArranged(trip.getItineraryArranged())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .build();
    }
}
