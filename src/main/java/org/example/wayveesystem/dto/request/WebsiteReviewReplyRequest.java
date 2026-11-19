package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebsiteReviewReplyRequest {

    @NotBlank(message = "Nội dung phản hồi không được để trống")
    String reply;
}
