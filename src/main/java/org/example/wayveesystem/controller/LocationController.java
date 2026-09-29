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
import org.example.wayveesystem.dto.response.LocationPageResponse;
import org.example.wayveesystem.dto.response.NominatimReverseResponse;
import org.example.wayveesystem.dto.response.ImageUploadResponse;
import org.example.wayveesystem.service.CloudinaryImageService;
import org.example.wayveesystem.service.LocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Location API", description = "Endpoints tìm kiếm hàng quán, nhà hàng, quán café gần vị trí người dùng qua OpenStreetMap & Overpass API")
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LocationController {

    LocationService locationService;
    CloudinaryImageService cloudinaryImageService;

    @Operation(summary = "Upload ảnh địa điểm lên Cloudinary")
        @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/api/v1/locations/images")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadLocationImage(
            @RequestParam("file") MultipartFile file) {
        ImageUploadResponse image = cloudinaryImageService.uploadLocationImage(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Location image uploaded successfully", image));
    }

    @Operation(summary = "Tìm địa điểm xung quanh (Hàng quán, Quán ăn, Quán cafe, ...)",
               description = "Tìm kiếm các địa điểm xung quanh vị trí tọa độ của User với bộ lọc loại hình mở rộng")
    @PostMapping("/api/v1/locations/nearby")
    public ResponseEntity<ApiResponse<LocationPageResponse>> searchNearby(
            @Valid @RequestBody LocationFilterRequest request) {
        LocationPageResponse locations = locationService.searchNearbyLocations(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Search nearby locations successful", locations));
    }

    @Operation(summary = "Lấy thông tin chi tiết địa điểm theo OSM ID",
               description = "Lấy chi tiết thông tin địa điểm từ OpenStreetMap theo OSM ID")
    @GetMapping("/api/v1/locations/osm/{osmId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getPlaceByOsmId(@PathVariable("osmId") Long osmId) {
        LocationResponse location = locationService.getPlaceByOsmId(osmId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Get location details successful", location));
    }

    @Operation(summary = "Reverse geocoding vị trí tọa độ (lat/lng)",
               description = "Truy vấn thông tin tên địa điểm từ tọa độ latitude và longitude qua Nominatim")
    @GetMapping({"/api/location/reverse", "/api/v1/locations/reverse"})
    public ResponseEntity<ApiResponse<NominatimReverseResponse>> reverseGeocode(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng) {
        NominatimReverseResponse response = locationService.reverseGeocode(lat, lng);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Reverse geocode successful", response));
    }
}
