package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.TripValidationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trip API", description = "Endpoints xử lý chuyến đi và validate thông tin lịch trình")
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    @Operation(summary = "Validate thông tin chuyến đi bước 1", description = "Kiểm tra tính hợp lệ của tất cả thông tin đầu vào ở Bước 1")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validateTripStep1(@Valid @RequestBody TripValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Thông tin chuyến đi bước 1 hợp lệ", null));
    }
}
