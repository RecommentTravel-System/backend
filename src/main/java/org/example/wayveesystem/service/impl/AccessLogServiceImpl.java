package org.example.wayveesystem.service.impl;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.dto.request.AccessLogRecordRequest;
import org.example.wayveesystem.dto.response.AccessLogResponse;
import org.example.wayveesystem.dto.response.TrafficStatsResponse;
import org.example.wayveesystem.model.AccessLog;
import org.example.wayveesystem.repository.AccessLogRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.AccessLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AccessLogServiceImpl implements AccessLogService {

    AccessLogRepository accessLogRepository;
    UserRepository userRepository;

    @Override
    @Transactional
    public AccessLogResponse recordAccess(AccessLogRecordRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = request.getUserAgent();
        if (userAgent == null && httpRequest != null) {
            userAgent = httpRequest.getHeader("User-Agent");
        }

        String deviceType = request.getDeviceType();
        if (deviceType == null || deviceType.isBlank()) {
            deviceType = parseDeviceType(userAgent);
        }

        String browser = request.getBrowser();
        if (browser == null || browser.isBlank()) {
            browser = parseBrowser(userAgent);
        }

        String os = request.getOperatingSystem();
        if (os == null || os.isBlank()) {
            os = parseOperatingSystem(userAgent);
        }

        String path = request.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }

        Long userId = request.getUserId();
        String userEmail = request.getUserEmail();
        String userFullName = request.getUserFullName();

        // Tự động gán thông tin người dùng từ SecurityContext nếu chưa có trong request
        if (userId == null || userEmail == null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    String principalName = auth.getName();
                    if (principalName != null && !principalName.isBlank()) {
                        var userOpt = userRepository.findByEmail(principalName);
                        if (userOpt.isPresent()) {
                            var u = userOpt.get();
                            if (userId == null) userId = u.getUserId();
                            if (userEmail == null) userEmail = u.getEmail();
                            if (userFullName == null) userFullName = u.getFullName();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Nếu truy cập các trang quản trị /admin mà chưa có user, gán với tài khoản Admin hệ thống
        if ((userId == null || userEmail == null) && path.startsWith("/admin")) {
            var adminOpt = userRepository.findByEmail("admin@gmail.com");
            if (adminOpt.isPresent()) {
                var admin = adminOpt.get();
                if (userId == null) userId = admin.getUserId();
                if (userEmail == null) userEmail = admin.getEmail();
                if (userFullName == null) userFullName = admin.getFullName();
            }
        }

        AccessLog logEntry = AccessLog.builder()
                .ipAddress(clientIp)
                .path(path)
                .pageTitle(request.getPageTitle())
                .method("GET")
                .deviceType(deviceType.toUpperCase())
                .browser(browser)
                .operatingSystem(os)
                .userAgent(userAgent)
                .userId(userId)
                .userEmail(userEmail)
                .userFullName(userFullName)
                .statusCode(200)
                .build();

        AccessLog saved = accessLogRepository.save(logEntry);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccessLogResponse> getAccessLogs(int page, int size, String keyword, String deviceType, String startDate, String endDate) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AccessLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String search = "%" + keyword.trim().toLowerCase() + "%";
                Predicate pathMatch = cb.like(cb.lower(root.get("path")), search);
                Predicate ipMatch = cb.like(cb.lower(root.get("ipAddress")), search);
                Predicate emailMatch = cb.like(cb.lower(root.get("userEmail")), search);
                Predicate nameMatch = cb.like(cb.lower(root.get("userFullName")), search);
                Predicate browserMatch = cb.like(cb.lower(root.get("browser")), search);
                predicates.add(cb.or(pathMatch, ipMatch, emailMatch, nameMatch, browserMatch));
            }

            if (deviceType != null && !deviceType.trim().isEmpty() && !"ALL".equalsIgnoreCase(deviceType)) {
                predicates.add(cb.equal(cb.upper(root.get("deviceType")), deviceType.trim().toUpperCase()));
            }

            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate.trim());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start.atStartOfDay()));
                } catch (Exception ignored) {}
            }

            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDate end = LocalDate.parse(endDate.trim());
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end.atTime(LocalTime.MAX)));
                } catch (Exception ignored) {}
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AccessLog> logs = accessLogRepository.findAll(spec, pageable);
        return logs.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TrafficStatsResponse getTrafficStats(String period) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long todayVisits = accessLogRepository.countByCreatedAtAfter(startOfToday);
        long todayUnique = accessLogRepository.countDistinctIpAddressByCreatedAtAfter(startOfToday);
        long totalVisits = accessLogRepository.count();
        long totalUnique = accessLogRepository.countDistinctIpAddress();

        int daysBack = "day".equalsIgnoreCase(period) ? 1 : "week".equalsIgnoreCase(period) ? 7 : 30;
        LocalDateTime since = LocalDate.now().minusDays(daysBack).atStartOfDay();

        // 1. Daily traffic
        List<TrafficStatsResponse.DailyPoint> dailyList = new ArrayList<>();
        List<Object[]> rawDaily = accessLogRepository.findDailyTrafficSince(since);
        for (Object[] row : rawDaily) {
            String dateStr = String.valueOf(row[0]);
            long visits = ((Number) row[1]).longValue();
            long uniqueIps = ((Number) row[2]).longValue();
            dailyList.add(TrafficStatsResponse.DailyPoint.builder()
                    .date(dateStr)
                    .visits(visits)
                    .uniqueVisitors(uniqueIps)
                    .build());
        }

        // 2. Top pages
        List<TrafficStatsResponse.PagePoint> topPages = new ArrayList<>();
        List<Object[]> rawPages = accessLogRepository.findTopVisitedPages(PageRequest.of(0, 7));
        for (Object[] row : rawPages) {
            topPages.add(TrafficStatsResponse.PagePoint.builder()
                    .path(String.valueOf(row[0]))
                    .pageTitle(row[1] != null ? String.valueOf(row[1]) : "Trang web")
                    .visits(((Number) row[2]).longValue())
                    .build());
        }

        // 3. Device breakdown
        List<TrafficStatsResponse.DevicePoint> devicePoints = new ArrayList<>();
        List<Object[]> rawDevices = accessLogRepository.findDeviceTypeCounts();
        long deviceTotal = 0;
        for (Object[] row : rawDevices) {
            deviceTotal += ((Number) row[1]).longValue();
        }
        for (Object[] row : rawDevices) {
            String dev = String.valueOf(row[0]);
            long cnt = ((Number) row[1]).longValue();
            double pct = deviceTotal > 0 ? Math.round((cnt * 100.0 / deviceTotal) * 10.0) / 10.0 : 0.0;
            devicePoints.add(TrafficStatsResponse.DevicePoint.builder()
                    .deviceType(dev)
                    .count(cnt)
                    .percentage(pct)
                    .build());
        }

        return TrafficStatsResponse.builder()
                .todayVisits(todayVisits)
                .todayUniqueVisitors(todayUnique)
                .totalVisits(totalVisits)
                .totalUniqueVisitors(totalUnique)
                .period(period)
                .dailyTraffic(dailyList)
                .topPages(topPages)
                .deviceBreakdown(devicePoints)
                .build();
    }

    @Override
    @Transactional
    public void deleteLog(Long id) {
        accessLogRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void clearOldLogs(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        accessLogRepository.deleteByCreatedAtBefore(cutoff);
    }

    private AccessLogResponse mapToResponse(AccessLog log) {
        return AccessLogResponse.builder()
                .id(log.getId())
                .ipAddress(log.getIpAddress())
                .path(log.getPath())
                .pageTitle(log.getPageTitle())
                .method(log.getMethod())
                .deviceType(log.getDeviceType())
                .browser(log.getBrowser())
                .operatingSystem(log.getOperatingSystem())
                .userAgent(log.getUserAgent())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .userFullName(log.getUserFullName())
                .statusCode(log.getStatusCode())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR",
                "X-Real-IP"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    private String parseDeviceType(String userAgent) {
        if (userAgent == null) return "DESKTOP";
        String lower = userAgent.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) {
            return "MOBILE";
        }
        if (lower.contains("ipad") || lower.contains("tablet")) {
            return "TABLET";
        }
        return "DESKTOP";
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "Chrome";
        String lower = userAgent.toLowerCase();
        if (lower.contains("edg")) return "Edge";
        if (lower.contains("chrome") && !lower.contains("chromium")) return "Chrome";
        if (lower.contains("safari") && !lower.contains("chrome")) return "Safari";
        if (lower.contains("firefox")) return "Firefox";
        if (lower.contains("opera") || lower.contains("opr")) return "Opera";
        return "Other";
    }

    private String parseOperatingSystem(String userAgent) {
        if (userAgent == null) return "Windows";
        String lower = userAgent.toLowerCase();
        if (lower.contains("windows")) return "Windows";
        if (lower.contains("mac os") || lower.contains("macintosh")) return "macOS";
        if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios")) return "iOS";
        if (lower.contains("android")) return "Android";
        if (lower.contains("linux")) return "Linux";
        return "Other";
    }
}
