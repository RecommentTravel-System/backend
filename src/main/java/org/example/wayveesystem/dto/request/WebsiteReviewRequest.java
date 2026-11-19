package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebsiteReviewRequest {

    @NotNull(message = "Điểm đánh giá là bắt buộc")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    Integer rating;

    String category;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    String comment;

    String userFullName;

    String userEmail;

    Boolean anonymous;

    List<String> mediaUrls;
}

