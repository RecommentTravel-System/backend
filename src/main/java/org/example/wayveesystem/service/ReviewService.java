package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.ReviewRequest;
import org.example.wayveesystem.dto.request.ReviewUpdateRequest;
import org.example.wayveesystem.dto.response.ReviewResponse;
import org.example.wayveesystem.dto.response.ReviewSummaryResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewRequest request);
    List<ReviewResponse> getReviewsByOsmId(Long osmId);
    List<ReviewResponse> getMyReviews();
    ReviewResponse getReviewById(Long reviewId);
    ReviewSummaryResponse getReviewSummaryByOsmId(Long osmId);
    ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request);
    void deleteReview(Long reviewId);
    List<ReviewResponse> searchReviews(Long osmId, Integer minRating);
}
