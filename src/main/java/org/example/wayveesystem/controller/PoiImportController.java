package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.PoiImportServerFileRequest;
import org.example.wayveesystem.dto.response.PoiImportResultResponse;
import org.example.wayveesystem.service.PoiImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin POI Import API", description = "Endpoints import dữ liệu địa điểm POI (OSM / Overture) dành cho Admin")
@RestController
@RequestMapping("/api/admin/poi-import")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*")
public class PoiImportController {

    PoiImportService poiImportService;

    @Operation(
            summary = "Import POI từ file CSV tải lên (Multipart)",
            description = "Đọc file CSV UTF-8 chứa dữ liệu OSM hoặc Overture và lưu vào catalog theo lô (Chỉ Admin)"
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PoiImportResultResponse>> importFromMultipart(
            @Parameter(description = "Nguồn dữ liệu: OSM hoặc OVERTURE", required = true)
            @RequestParam("source") LocationSource source,

            @Parameter(
                    description = "File CSV định dạng UTF-8 (header: source_id,name,category,address,lat,lon,confidence,operating_status)",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(type = "string", format = "binary"))
            )
            @RequestParam("file") MultipartFile file
    ) {
        PoiImportResultResponse result = poiImportService.importFromMultipart(source, file);
        return ResponseEntity.ok(ApiResponse.success("POI import finished successfully", result));
    }

    @Operation(
            summary = "Import POI từ đường dẫn file trên Server",
            description = "Nhập dữ liệu POI từ file CSV đã có sẵn trên máy chủ (Chỉ Admin)"
    )
    @PostMapping("/server-file")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PoiImportResultResponse>> importFromServerFile(
            @Valid @RequestBody PoiImportServerFileRequest request
    ) {
        PoiImportResultResponse result = poiImportService.importFromServerFile(request.source(), request.filePath());
        return ResponseEntity.ok(ApiResponse.success("Server POI import finished successfully", result));
    }
}
