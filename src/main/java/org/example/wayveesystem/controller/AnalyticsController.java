package org.example.wayveesystem.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.response.AnalyticsSummaryResponse;
import org.example.wayveesystem.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*")
public class AnalyticsController {

    AnalyticsService analyticsService;

    /**
     * GET /api/admin/analytics?period=day|week|month
     * Returns KPI totals + chart series grouped by the requested period.
     * Requires ROLE_ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getAnalytics(
            @RequestParam(defaultValue = "month") String period) {

        AnalyticsSummaryResponse data = analyticsService.getAnalytics(period);
        return ResponseEntity.ok(ApiResponse.success("Analytics data retrieved successfully", data));
    }
}
