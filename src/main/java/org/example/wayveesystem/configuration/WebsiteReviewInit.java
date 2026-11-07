package org.example.wayveesystem.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.model.WebsiteReview;
import org.example.wayveesystem.repository.WebsiteReviewRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class WebsiteReviewInit {

    @Bean
    ApplicationRunner websiteReviewRunner(WebsiteReviewRepository repository) {
        return application -> {
            try {
                if (repository.count() == 0) {
                    List<WebsiteReview> initial = List.of(
                            WebsiteReview.builder()
                                    .userFullName("Nguyễn Văn An")
                                    .userEmail("an.nguyen@example.com")
                                    .rating(5)
                                    .category("Giao diện & Trải nghiệm")
                                    .comment("Giao diện trang web rất hiện đại và tinh tế, phối màu hài hòa. Các thao tác tìm kiếm địa điểm và lập lịch trình rất mượt mà.")
                                    .anonymous(false)
                                    .build(),
                            WebsiteReview.builder()
                                    .userFullName("Trần Thị Mai")
                                    .userEmail("mai.tran@example.com")
                                    .rating(5)
                                    .category("Tính năng lịch trình")
                                    .comment("Tính năng tạo lịch trình thông minh quá tuyệt vời! Tiết kiệm cho mình cả buổi chiều lên kế hoạch đi du lịch Đà Nẵng cùng nhóm bạn.")
                                    .anonymous(false)
                                    .build(),
                            WebsiteReview.builder()
                                    .userFullName("Lê Hoàng Quân")
                                    .userEmail("quan.le@example.com")
                                    .rating(4)
                                    .category("Tốc độ & Hiệu năng")
                                    .comment("Tốc độ tải bản đồ và định tuyến rất nhanh. Bản đồ hiển thị rõ ràng, nếu bổ sung thêm chế độ xem 3D nữa thì hoàn hảo.")
                                    .anonymous(false)
                                    .build(),
                            WebsiteReview.builder()
                                    .userFullName("Người dùng ẩn danh")
                                    .userEmail(null)
                                    .rating(5)
                                    .category("Trải nghiệm chung")
                                    .comment("Trang web cung cấp đầy đủ thông tin địa điểm cần thiết, phân loại danh mục rõ ràng, rất dễ tra cứu.")
                                    .anonymous(true)
                                    .build(),
                            WebsiteReview.builder()
                                    .userFullName("Phạm Thùy Linh")
                                    .userEmail("linh.pham@example.com")
                                    .rating(4)
                                    .category("Gợi ý địa điểm")
                                    .comment("Các quán ăn và điểm check-in được gợi ý rất chuẩn gu giới trẻ. Rất mong đội ngũ phát triển thêm ứng dụng trên điện thoại.")
                                    .anonymous(false)
                                    .build()
                    );
                    repository.saveAll(initial);
                    log.info("Initialized {} sample website reviews", initial.size());
                }
            } catch (Exception e) {
                log.warn("Could not initialize website reviews: {}", e.getMessage());
            }
        };
    }
}

