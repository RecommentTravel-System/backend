package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.request.ReviewRequest;
import org.example.wayveesystem.dto.request.ReviewUpdateRequest;
import org.example.wayveesystem.dto.response.ReviewResponse;
import org.example.wayveesystem.model.Review;
import org.example.wayveesystem.model.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toReview(ReviewRequest request, User user) {
        if (request == null) {
            return null;
        }
        return Review.builder()
                .user(user)
                .osmId(request.getOsmId())
                .placeName(request.getPlaceName())
                .rating(request.getRating())
                .comment(request.getComment())
                .anonymous(request.getAnonymous() != null ? request.getAnonymous() : false)
                .build();
    }

    public ReviewResponse toReviewResponse(Review review) {
        if (review == null) {
            return null;
        }
        boolean isAnonymous = Boolean.TRUE.equals(review.getAnonymous());
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .userId(isAnonymous ? null : (review.getUser() != null ? review.getUser().getUserId() : null))
                .userFullName(isAnonymous ? "Ẩn danh" : (review.getUser() != null ? review.getUser().getFullName() : null))
                .osmId(review.getOsmId())
                .placeName(review.getPlaceName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .anonymous(isAnonymous)
                .build();
    }

    public void updateReview(Review review, ReviewUpdateRequest request) {
        if (review == null || request == null) {
            return;
        }
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        if (request.getAnonymous() != null) {
            review.setAnonymous(request.getAnonymous());
        }
    }
}
