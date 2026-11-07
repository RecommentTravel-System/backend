package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.WebsiteReviewRequest;
import org.example.wayveesystem.dto.response.WebsiteReviewResponse;
import org.example.wayveesystem.dto.response.WebsiteReviewStatsResponse;

import java.util.List;

public interface WebsiteReviewService {
    WebsiteReviewResponse createWebsiteReview(WebsiteReviewRequest request);
    List<WebsiteReviewResponse> getAllWebsiteReviews(Integer rating, String category, String keyword);
    WebsiteReviewStatsResponse getWebsiteReviewStats();
    List<WebsiteReviewResponse> getMyWebsiteReviews();
}

