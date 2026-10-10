package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.LocationStatusUpdateRequest;
import org.example.wayveesystem.dto.request.ReplanConfirmRequest;
import org.example.wayveesystem.dto.request.ReplanPreviewRequest;
import org.example.wayveesystem.dto.response.ReplanProposalResponse;
import org.example.wayveesystem.dto.response.TripLocationResponse;
import org.example.wayveesystem.dto.response.TripResponse;
import org.example.wayveesystem.service.TripReplanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Trip Re-planning API", description = "Endpoints xử lý tính toán và sắp xếp lại lịch trình nhiều ngày khi dở dang")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TripReplanningController {

    TripReplanningService replanningService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Preview re-plan proposal", description = "Tính toán đề xuất sắp xếp lại các điểm chưa đi sang các ngày còn lại (In-memory simulation, chưa ghi DB)")
    @PostMapping("/{tripId}/replan/preview")
    public ResponseEntity<ApiResponse<ReplanProposalResponse>> previewReplan(
            @PathVariable("tripId") Long tripId,
            @Valid @RequestBody ReplanPreviewRequest request) {
        ReplanProposalResponse response = replanningService.previewReplan(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("Replan proposal generated successfully", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Confirm re-plan proposal", description = "Xác nhận và lưu đề xuất re-plan vào database")
    @PostMapping("/{tripId}/replan/confirm")
    public ResponseEntity<ApiResponse<TripResponse>> confirmReplan(
            @PathVariable("tripId") Long tripId,
            @Valid @RequestBody ReplanConfirmRequest request) {
        TripResponse response = replanningService.confirmReplan(tripId, request);
        return ResponseEntity.ok(ApiResponse.success("Replan proposal confirmed and applied successfully", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update trip location status", description = "Cập nhật trạng thái điểm (VISITED, SKIPPED, POSTPONED) hoặc check-in theo bán kính GPS 100m")
    @PatchMapping("/{tripId}/locations/{locationId}/status")
    public ResponseEntity<ApiResponse<TripLocationResponse>> updateLocationStatus(
            @PathVariable("tripId") Long tripId,
            @PathVariable("locationId") Long locationId,
            @RequestBody LocationStatusUpdateRequest request) {
        TripLocationResponse response = replanningService.updateLocationStatus(tripId, locationId, request);
        return ResponseEntity.ok(ApiResponse.success("Location status updated successfully", response));
    }
}
