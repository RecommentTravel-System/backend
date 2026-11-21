package org.example.wayveesystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.AccessLogRecordRequest;
import org.example.wayveesystem.dto.response.AccessLogResponse;
import org.example.wayveesystem.dto.response.TrafficStatsResponse;
import org.example.wayveesystem.service.AccessLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*")
public class AccessLogController {

    AccessLogService accessLogService;

    /**
     * POST /api/access-logs/record
     * Public endpoint invoked by frontend router on page change to record page view.
     */
    @PostMapping("/access-logs/record")
    public ResponseEntity<ApiResponse<AccessLogResponse>> recordAccess(
            @Valid @RequestBody AccessLogRecordRequest request,
            HttpServletRequest httpRequest) {
        AccessLogResponse response = accessLogService.recordAccess(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Access recorded successfully", response));
    }

    /**
     * GET /api/admin/access-logs
     * Admin view paginated & filtered access logs.
     */
    @GetMapping("/admin/access-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AccessLogResponse>>> getAccessLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<AccessLogResponse> result = accessLogService.getAccessLogs(page, size, keyword, deviceType, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Access logs retrieved successfully", result));
    }

    /**
     * GET /api/admin/access-logs/stats
     * Aggregated traffic stats: today visits, unique visitors, daily series, top pages, device breakdown.
     */
    @GetMapping("/admin/access-logs/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TrafficStatsResponse>> getTrafficStats(
            @RequestParam(defaultValue = "month") String period) {
        TrafficStatsResponse stats = accessLogService.getTrafficStats(period);
        return ResponseEntity.ok(ApiResponse.success("Traffic stats retrieved successfully", stats));
    }

    /**
     * DELETE /api/admin/access-logs/{id}
     * Admin delete a specific log entry.
     */
    @DeleteMapping("/admin/access-logs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAccessLog(@PathVariable Long id) {
        accessLogService.deleteLog(id);
        return ResponseEntity.ok(ApiResponse.success("Access log deleted successfully"));
    }
}
