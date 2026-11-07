package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebsiteReviewStatsResponse {

    long totalReviews;
    double averageRating;
    Map<Integer, Long> ratingCounts;
    double positivePercentage;
}

