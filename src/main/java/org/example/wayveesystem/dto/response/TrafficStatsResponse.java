package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrafficStatsResponse {
    long todayVisits;
    long todayUniqueVisitors;
    long totalVisits;
    long totalUniqueVisitors;
    String period;

    List<DailyPoint> dailyTraffic;
    List<PagePoint> topPages;
    List<DevicePoint> deviceBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPoint {
        String date;            // "YYYY-MM-DD"
        long visits;            // total page views on this day
        long uniqueVisitors;    // unique IPs on this day
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagePoint {
        String path;
        String pageTitle;
        long visits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DevicePoint {
        String deviceType;      // "DESKTOP", "MOBILE", "TABLET"
        long count;
        double percentage;
    }
}
