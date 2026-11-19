package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

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
    List<String> mediaUrls;
    String adminReply;
    LocalDateTime repliedAt;
    String repliedBy;
    LocalDateTime createdAt;
}

