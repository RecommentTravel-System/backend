package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.response.AnalyticsSummaryResponse;

public interface AnalyticsService {
    /**
     * Returns analytics summary aggregated by the given period.
     * @param period "day" | "week" | "month"
     * @return AnalyticsSummaryResponse with KPIs and chart series
     */
    AnalyticsSummaryResponse getAnalytics(String period);
}
