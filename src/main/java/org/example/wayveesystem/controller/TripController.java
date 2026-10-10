package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.TripRequest;
import org.example.wayveesystem.dto.request.TripValidationRequest;
import org.example.wayveesystem.dto.response.TripResponse;
import org.example.wayveesystem.dto.request.ItineraryOptimizeRequest;
import org.example.wayveesystem.dto.response.ItineraryOptimizeResponse;
import org.example.wayveesystem.service.TripOptimizationService;
import org.example.wayveesystem.service.TripService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Trip API", description = "Endpoints xử lý chuyến đi và validate thông tin lịch trình")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripController {

    TripService tripService;
    TripOptimizationService tripOptimizationService;

    @Operation(summary = "Tự động tối ưu hóa lịch trình các ngày", description = "Phân cụm các điểm vào các ngày bằng K-means cân bằng và sắp xếp thứ tự di chuyển tối ưu bằng Nearest Neighbor + 2-opt")
    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<ItineraryOptimizeResponse>> optimizeItinerary(
            @Valid @RequestBody ItineraryOptimizeRequest request) {
        ItineraryOptimizeResponse response = tripOptimizationService.optimizeItinerary(request);
        return ResponseEntity.ok(ApiResponse.success("Tối ưu hóa lịch trình thành công", response));
    }

    @Operation(summary = "Validate thông tin chuyến đi bước 1", description = "Kiểm tra tính hợp lệ của tất cả thông tin đầu vào ở Bước 1")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validateTripStep1(@Valid @RequestBody TripValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thông tin chuyến đi bước 1 hợp lệ", null));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a trip")
    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody TripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created successfully", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get my active trips")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips() {
        return ResponseEntity.ok(ApiResponse.success("Get trips successful", tripService.getMyTrips()));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get trip detail")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable("id") Long tripId) {
        return ResponseEntity.ok(ApiResponse.success("Get trip successful", tripService.getTripById(tripId)));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update a trip")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(
            @PathVariable("id") Long tripId,
            @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Trip updated successfully", tripService.updateTrip(tripId, request)));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Confirm arranged itinerary")
    @PatchMapping("/{id}/itinerary/confirm")
    public ResponseEntity<ApiResponse<TripResponse>> confirmItinerary(@PathVariable("id") Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Itinerary confirmed successfully", tripService.confirmItinerary(tripId)));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cancel a trip")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTrip(@PathVariable("id") Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Trip cancelled successfully", tripService.cancelTrip(tripId)));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Soft delete a trip")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(@PathVariable("id") Long tripId) {
        tripService.deleteTrip(tripId);
        return ResponseEntity.ok(ApiResponse.success("Trip deleted successfully"));
    }
}
