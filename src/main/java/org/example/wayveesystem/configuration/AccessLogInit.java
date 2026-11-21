package org.example.wayveesystem.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.model.AccessLog;
import org.example.wayveesystem.repository.AccessLogRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class AccessLogInit {

    @Bean
    ApplicationRunner accessLogRunner(AccessLogRepository repository) {
        return args -> {
            try {
                if (repository.count() == 0) {
                    log.info("Seeding realistic sample Access Logs for admin traffic analysis...");
                    List<AccessLog> logs = new ArrayList<>();
                    Random random = new Random(42);

                    String[] paths = {
                            "/", "/places", "/trip/plan", "/trip/info",
                            "/reviews", "/pricing", "/support", "/login", "/profile"
                    };
                    String[] titles = {
                            "Trang chủ | Wayvee", "Khám phá địa điểm du lịch | Wayvee",
                            "Tạo lịch trình thông minh | Wayvee", "Chi tiết hành trình | Wayvee",
                            "Đánh giá từ cộng đồng | Wayvee", "Bảng giá & Gói dịch vụ | Wayvee",
                            "Trung tâm hỗ trợ | Wayvee", "Đăng nhập | Wayvee", "Hồ sơ cá nhân | Wayvee"
                    };
                    String[] ips = {
                            "118.69.182.45", "14.161.22.90", "113.160.225.12", "171.244.33.104",
                            "123.30.155.80", "115.79.138.22", "42.112.89.51", "125.235.21.6"
                    };
                    String[] devices = {"Desktop", "Mobile", "Tablet"};
                    String[] browsers = {"Chrome", "Safari", "Edge", "Firefox"};
                    String[] osList = {"Windows 10/11", "iOS", "Android", "macOS"};

                    // Seed traffic across the past 14 days
                    LocalDateTime now = LocalDateTime.now();
                    for (int day = 13; day >= 0; day--) {
                        // Generate between 8 and 25 visits per day
                        int visitsToday = 10 + random.nextInt(15);
                        for (int i = 0; i < visitsToday; i++) {
                            int pathIdx = random.nextInt(paths.length);
                            int devIdx = random.nextInt(10);
                            String device = (devIdx < 6) ? "Desktop" : (devIdx < 9 ? "Mobile" : "Tablet");
                            String os = device.equals("Mobile") ? (random.nextBoolean() ? "iOS" : "Android")
                                    : (device.equals("Tablet") ? "iPadOS" : (random.nextBoolean() ? "Windows 10/11" : "macOS"));
                            String browser = os.equals("iOS") ? "Safari" : (random.nextBoolean() ? "Chrome" : "Edge");
                            String ip = ips[random.nextInt(ips.length)];

                            int hour = 7 + random.nextInt(16);
                            int minute = random.nextInt(60);
                            int second = random.nextInt(60);
                            LocalDateTime visitTime = now.minusDays(day).withHour(hour).withMinute(minute).withSecond(second);

                            AccessLog logEntry = AccessLog.builder()
                                    .ipAddress(ip)
                                    .path(paths[pathIdx])
                                    .pageTitle(titles[pathIdx])
                                    .deviceType(device)
                                    .browser(browser)
                                    .operatingSystem(os)
                                    .userAgent("Mozilla/5.0 (" + os + ") AppleWebKit/537.36 (KHTML, like Gecko) " + browser + "/120.0.0.0 Safari/537.36")
                                    .statusCode(200)
                                    .createdAt(visitTime)
                                    .build();

                            // Occasional logged-in user
                            if (random.nextInt(3) == 0) {
                                logEntry.setUserEmail("user" + (random.nextInt(5) + 1) + "@example.com");
                                logEntry.setUserFullName("Thành viên " + (random.nextInt(5) + 1));
                            }

                            logs.add(logEntry);
                        }
                    }

                    repository.saveAll(logs);
                    log.info("Successfully seeded {} access log entries.", logs.size());
                }
            } catch (Exception e) {
                log.warn("Could not seed access logs: {}", e.getMessage());
            }
        };
    }
}
