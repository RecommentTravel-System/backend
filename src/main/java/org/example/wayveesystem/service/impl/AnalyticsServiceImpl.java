package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.PaymentStatus;
import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.dto.response.AnalyticsSummaryResponse;
import org.example.wayveesystem.dto.response.AnalyticsSummaryResponse.PeriodPoint;
import org.example.wayveesystem.repository.PaymentRepository;
import org.example.wayveesystem.repository.SubscriptionRepository;
import org.example.wayveesystem.repository.TripRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.AnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    UserRepository userRepository;
    TripRepository tripRepository;
    PaymentRepository paymentRepository;
    SubscriptionRepository subscriptionRepository;

    @Override
    public AnalyticsSummaryResponse getAnalytics(String period) {
        String normalizedPeriod = (period == null) ? "month" : period.toLowerCase();
        LocalDateTime from = resolveFrom(normalizedPeriod);

        // ── KPI totals ──────────────────────────────────────────────────────
        long totalUsers = userRepository.count();
        long totalTrips = tripRepository.count();
        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE).size();

        // ── KPI deltas for current period ───────────────────────────────────
        long newUsersThisPeriod = userRepository.countByCreatedAtAfter(from);
        long newTripsThisPeriod = tripRepository.countByCreatedAtAfter(from);
        BigDecimal newRevenueThisPeriod = paymentRepository.sumAmountByStatusFrom(PaymentStatus.SUCCESS, from);
        if (newRevenueThisPeriod == null) newRevenueThisPeriod = BigDecimal.ZERO;

        // ── Chart series ────────────────────────────────────────────────────
        List<PeriodPoint> revenueByPeriod;
        List<PeriodPoint> usersByPeriod;
        List<PeriodPoint> tripsByPeriod;

        switch (normalizedPeriod) {
            case "day" -> {
                revenueByPeriod = toPoints(paymentRepository.revenueGroupedByDay(from), "day");
                usersByPeriod   = toPoints(userRepository.usersGroupedByDay(from), "day");
                tripsByPeriod   = toPoints(tripRepository.tripsGroupedByDay(from), "day");
            }
            case "week" -> {
                revenueByPeriod = toPoints(paymentRepository.revenueGroupedByWeek(from), "week");
                usersByPeriod   = toPoints(userRepository.usersGroupedByWeek(from), "week");
                tripsByPeriod   = toPoints(tripRepository.tripsGroupedByWeek(from), "week");
            }
            default -> { // month
                revenueByPeriod = toPoints(paymentRepository.revenueGroupedByMonth(from), "month");
                usersByPeriod   = toPoints(userRepository.usersGroupedByMonth(from), "month");
                tripsByPeriod   = toPoints(tripRepository.tripsGroupedByMonth(from), "month");
            }
        }

        return AnalyticsSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalTrips(totalTrips)
                .totalRevenue(totalRevenue)
                .activeSubscriptions(activeSubscriptions)
                .newUsersThisPeriod(newUsersThisPeriod)
                .newTripsThisPeriod(newTripsThisPeriod)
                .newRevenueThisPeriod(newRevenueThisPeriod)
                .period(normalizedPeriod)
                .revenueByPeriod(revenueByPeriod)
                .usersByPeriod(usersByPeriod)
                .tripsByPeriod(tripsByPeriod)
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private LocalDateTime resolveFrom(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case "day"  -> now.minusDays(30);   // last 30 days
            case "week" -> now.minusWeeks(12);  // last 12 weeks
            default     -> now.minusMonths(12); // last 12 months
        };
    }

    /**
     * Converts raw Object[] rows into PeriodPoint list.
     *
     * For "day":   row = [date_string | java.sql.Date, value]
     * For "week":  row = [year, week, value]
     * For "month": row = [year, month, value]
     */
    private List<PeriodPoint> toPoints(List<Object[]> rows, String granularity) {
        List<PeriodPoint> result = new ArrayList<>();
        String[] monthNames = {"T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"};

        for (Object[] row : rows) {
            String label;
            double value;

            if ("day".equals(granularity)) {
                // row[0] = date (java.sql.Date or String), row[1] = amount/count
                label = row[0] != null ? row[0].toString() : "?";
                value = toDouble(row[1]);
            } else if ("week".equals(granularity)) {
                // row[0] = year, row[1] = week, row[2] = amount/count
                int weekNum = row[1] != null ? ((Number) row[1]).intValue() : 0;
                label = "Tuần " + weekNum;
                value = toDouble(row[2]);
            } else {
                // month: row[0] = year, row[1] = month (1-12), row[2] = amount/count
                int monthIdx = row[1] != null ? ((Number) row[1]).intValue() - 1 : 0;
                label = monthNames[Math.max(0, Math.min(11, monthIdx))];
                value = toDouble(row[2]);
            }

            result.add(PeriodPoint.builder().label(label).value(value).build());
        }
        return result;
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (Exception e) { return 0.0; }
    }
}
