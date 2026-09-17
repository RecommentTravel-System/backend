package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.LocationFilterRequest;
import org.example.wayveesystem.dto.response.LocationResponse;
import org.example.wayveesystem.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Location API", description = "Endpoints tìm kiếm hàng quán, nhà hàng, quán café gần vị trí người dùng qua OpenStreetMap & Overpass API")
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LocationController {

    LocationService locationService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Tìm địa điểm xung quanh (Hàng quán, Quán ăn, Quán cafe, ...)",
               description = "Tìm kiếm các địa điểm xung quanh vị trí tọa độ của User với bộ lọc loại hình mở rộng")
    @PostMapping("/nearby")
    public ResponseEntity<ApiResponse<List<LocationResponse>>> searchNearby(
            @Valid @RequestBody LocationFilterRequest request) {
        List<LocationResponse> locations = locationService.searchNearbyLocations(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Search nearby locations successful", locations));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy thông tin chi tiết địa điểm theo OSM ID",
               description = "Lấy chi tiết thông tin địa điểm từ OpenStreetMap theo OSM ID")
    @GetMapping("/osm/{osmId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getPlaceByOsmId(@PathVariable("osmId") Long osmId) {
        LocationResponse location = locationService.getPlaceByOsmId(osmId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Get location details successful", location));
    }
}
