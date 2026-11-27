package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.request.WebsiteReviewRequest;
import org.example.wayveesystem.dto.response.ImageUploadResponse;
import org.example.wayveesystem.dto.response.WebsiteReviewResponse;
import org.example.wayveesystem.dto.response.WebsiteReviewStatsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WebsiteReviewService {
    WebsiteReviewResponse createWebsiteReview(WebsiteReviewRequest request);
    WebsiteReviewResponse updateWebsiteReview(Long id, WebsiteReviewRequest request);
    void deleteWebsiteReview(Long id);
    List<WebsiteReviewResponse> getAllWebsiteReviews(Integer rating, String category, String keyword);
    WebsiteReviewStatsResponse getWebsiteReviewStats();
    List<WebsiteReviewResponse> getMyWebsiteReviews();
    WebsiteReviewResponse replyWebsiteReview(Long id, String reply);
    WebsiteReviewResponse deleteReply(Long id);
    ImageUploadResponse uploadMedia(MultipartFile file);
}
