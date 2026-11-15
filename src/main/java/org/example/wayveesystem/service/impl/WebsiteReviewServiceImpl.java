package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.dto.request.WebsiteReviewRequest;
import org.example.wayveesystem.dto.response.WebsiteReviewResponse;
import org.example.wayveesystem.dto.response.WebsiteReviewStatsResponse;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.model.WebsiteReview;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.repository.WebsiteReviewRepository;
import org.example.wayveesystem.service.WebsiteReviewService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebsiteReviewServiceImpl implements WebsiteReviewService {

    WebsiteReviewRepository websiteReviewRepository;
    UserRepository userRepository;

    private User getCurrentUserOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
                return null;
            }
            return userRepository.findById(Long.valueOf(auth.getName())).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return false;
            }
            boolean hasAdminAuthority = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority())
                                || "ADMIN".equalsIgnoreCase(a.getAuthority()));
            if (hasAdminAuthority) {
                return true;
            }
            User currentUser = getCurrentUserOrNull();
            return currentUser != null && currentUser.getRole() == org.example.wayveesystem.common.enums.Role.ADMIN;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public WebsiteReviewResponse createWebsiteReview(WebsiteReviewRequest request) {
        User currentUser = getCurrentUserOrNull();
        boolean isAnonymous = Boolean.TRUE.equals(request.getAnonymous());

        String fullName = request.getUserFullName();
        String email = request.getUserEmail();

        if (currentUser != null) {
            if (fullName == null || fullName.isBlank()) {
                fullName = currentUser.getFullName();
            }
            if (email == null || email.isBlank()) {
                email = currentUser.getEmail();
            }
        }

        if (fullName == null || fullName.isBlank()) {
            fullName = isAnonymous ? "Người dùng ẩn danh" : "Khách tham quan";
        }

        WebsiteReview review = WebsiteReview.builder()
                .user(currentUser)
                .userFullName(fullName)
                .userEmail(email)
                .rating(request.getRating())
                .category(request.getCategory() != null && !request.getCategory().isBlank() ? request.getCategory() : "Trải nghiệm chung")
                .comment(request.getComment() != null ? request.getComment().trim() : "")
                .anonymous(isAnonymous)
                .build();

        WebsiteReview saved = websiteReviewRepository.save(review);
        return toResponse(saved);
    }

    @Override
    public List<WebsiteReviewResponse> getAllWebsiteReviews(Integer rating, String category, String keyword) {
        List<WebsiteReview> list;
        if (isAdmin()) {
            // Admin có quyền xem tất cả đánh giá trang web từ mọi người dùng
            list = (rating != null && rating > 0)
                    ? websiteReviewRepository.findByRatingOrderByCreatedAtDesc(rating)
                    : websiteReviewRepository.findAllByOrderByCreatedAtDesc();
        } else {
            // Người dùng thông thường: CHỈ xem được các đánh giá do chính mình viết
            User currentUser = getCurrentUserOrNull();
            if (currentUser == null) {
                // Khách chưa đăng nhập: không xem đánh giá của người khác
                return List.of();
            }
            list = websiteReviewRepository.findByUserOrEmail(currentUser.getUserId(), currentUser.getEmail());
            if (rating != null && rating > 0) {
                list = list.stream().filter(r -> r.getRating() != null && r.getRating().equals(rating)).toList();
            }
        }

        String kw = keyword != null ? keyword.trim().toLowerCase() : "";
        String cat = category != null ? category.trim().toLowerCase() : "";

        return list.stream()
                .filter(r -> {
                    if (!cat.isEmpty() && !"all".equalsIgnoreCase(cat)) {
                        if (r.getCategory() == null || !r.getCategory().toLowerCase().contains(cat)) {
                            return false;
                        }
                    }
                    if (!kw.isEmpty()) {
                        String name = r.getUserFullName() != null ? r.getUserFullName().toLowerCase() : "";
                        String email = r.getUserEmail() != null ? r.getUserEmail().toLowerCase() : "";
                        String comment = r.getComment() != null ? r.getComment().toLowerCase() : "";
                        return name.contains(kw) || email.contains(kw) || comment.contains(kw);
                    }
                    return true;
                })
                .map(this::toResponse)
                .toList();
    }

    @Override
    public WebsiteReviewStatsResponse getWebsiteReviewStats() {
        if (isAdmin()) {
            // Admin xem toàn bộ thống kê hệ thống
            long total = websiteReviewRepository.count();
            Double avg = websiteReviewRepository.getAverageRating();
            double averageRating = (avg != null && total > 0) ? Math.round(avg * 10.0) / 10.0 : 5.0;

            Map<Integer, Long> counts = new HashMap<>();
            long positiveCount = 0;
            for (int i = 1; i <= 5; i++) {
                long c = websiteReviewRepository.countByRating(i);
                counts.put(i, c);
                if (i >= 4) {
                    positiveCount += c;
                }
            }

            double positivePercent = total > 0 ? Math.round(((double) positiveCount / total) * 1000.0) / 10.0 : 100.0;

            return WebsiteReviewStatsResponse.builder()
                    .totalReviews(total)
                    .averageRating(averageRating)
                    .ratingCounts(counts)
                    .positivePercentage(positivePercent)
                    .build();
        }

        // Người dùng thường: chỉ tính thống kê trên các đánh giá của chính mình
        User currentUser = getCurrentUserOrNull();
        if (currentUser == null) {
            Map<Integer, Long> counts = new HashMap<>();
            for (int i = 1; i <= 5; i++) counts.put(i, 0L);
            return WebsiteReviewStatsResponse.builder()
                    .totalReviews(0L)
                    .averageRating(5.0)
                    .ratingCounts(counts)
                    .positivePercentage(100.0)
                    .build();
        }

        List<WebsiteReview> myReviews = websiteReviewRepository.findByUserOrEmail(currentUser.getUserId(), currentUser.getEmail());
        long total = myReviews.size();
        double avg = total > 0 ? myReviews.stream().mapToInt(r -> r.getRating() != null ? r.getRating() : 5).average().orElse(5.0) : 5.0;

        Map<Integer, Long> counts = new HashMap<>();
        long positiveCount = 0;
        for (int i = 1; i <= 5; i++) {
            final int star = i;
            long c = myReviews.stream().filter(r -> r.getRating() != null && r.getRating().equals(star)).count();
            counts.put(i, c);
            if (i >= 4) positiveCount += c;
        }

        double positivePercent = total > 0 ? Math.round(((double) positiveCount / total) * 1000.0) / 10.0 : 100.0;

        return WebsiteReviewStatsResponse.builder()
                .totalReviews(total)
                .averageRating(Math.round(avg * 10.0) / 10.0)
                .ratingCounts(counts)
                .positivePercentage(positivePercent)
                .build();
    }

    @Override
    public List<WebsiteReviewResponse> getMyWebsiteReviews() {
        User currentUser = getCurrentUserOrNull();
        if (currentUser == null) {
            return List.of();
        }
        return websiteReviewRepository.findByUserOrEmail(currentUser.getUserId(), currentUser.getEmail()).stream()
                .map(this::toResponse)
                .toList();
    }

    private WebsiteReviewResponse toResponse(WebsiteReview r) {
        String displayName = r.getUserFullName();
        String displayEmail = r.getUserEmail();
        if (Boolean.TRUE.equals(r.getAnonymous())) {
            displayName = "Người dùng ẩn danh";
            displayEmail = null;
        }

        return WebsiteReviewResponse.builder()
                .id(r.getId())
                .userId(r.getUser() != null ? r.getUser().getUserId() : null)
                .userFullName(displayName)
                .userEmail(displayEmail)
                .rating(r.getRating())
                .category(r.getCategory())
                .comment(r.getComment())
                .anonymous(r.getAnonymous())
                .createdAt(r.getCreatedAt())
                .build();
    }
}

