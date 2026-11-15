package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebsiteReviewResponse {

    Long id;
    Long userId;
    String userFullName;
    String userEmail;
    Integer rating;
    String category;
    String comment;
    Boolean anonymous;
    LocalDateTime createdAt;
}

