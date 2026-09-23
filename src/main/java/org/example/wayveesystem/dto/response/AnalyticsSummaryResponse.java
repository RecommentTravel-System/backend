package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnalyticsSummaryResponse {

    // KPI Summary Cards
    long totalUsers;
    long totalTrips;
    BigDecimal totalRevenue;
    long activeSubscriptions;
    long newUsersThisPeriod;
    long newTripsThisPeriod;
    BigDecimal newRevenueThisPeriod;

    // Period: "day" | "week" | "month"
    String period;

    // Chart data series
    List<PeriodPoint> revenueByPeriod;
    List<PeriodPoint> usersByPeriod;
    List<PeriodPoint> tripsByPeriod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodPoint {
        String label;    // e.g. "2024-09-01" or "T1" or "Tuần 1"
        double value;    // numeric value for the chart
    }
}
