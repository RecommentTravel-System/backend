package org.example.wayveesystem.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class WebsiteReviewInit {
    // Dữ liệu đánh giá trang web đã được chuyển trực tiếp vào cơ sở dữ liệu qua file SQL.
    // Đã bỏ mock dữ liệu tự động khi khởi động ứng dụng.
}
