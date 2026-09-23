package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.request.ReviewRequest;
import org.example.wayveesystem.dto.request.ReviewUpdateRequest;
import org.example.wayveesystem.dto.response.ReviewResponse;
import org.example.wayveesystem.dto.response.ReviewSummaryResponse;
import org.example.wayveesystem.mapper.ReviewMapper;
import org.example.wayveesystem.model.Review;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.ReviewRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.ReviewService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewServiceImpl implements ReviewService {

    ReviewRepository reviewRepository;
    UserRepository userRepository;
    ReviewMapper reviewMapper;

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkPermission(User currentUser, Review review) {
        if (!review.getUser().getUserId().equals(currentUser.getUserId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        User currentUser = getCurrentUser();
        Review review = reviewMapper.toReview(request, currentUser);
        review = reviewRepository.save(review);
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    public List<ReviewResponse> getReviewsByOsmId(Long osmId) {
        return reviewRepository.findByOsmId(osmId).stream()
                .map(reviewMapper::toReviewResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getMyReviews() {
        User currentUser = getCurrentUser();
        return reviewRepository.findByUser(currentUser).stream()
                .map(reviewMapper::toReviewResponse)
                .toList();
    }

    @Override
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    public ReviewSummaryResponse getReviewSummaryByOsmId(Long osmId) {
        Double avg = reviewRepository.getAverageRatingByOsmId(osmId);
        long count = reviewRepository.countByOsmId(osmId);

        return ReviewSummaryResponse.builder()
                .osmId(osmId)
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .totalReviews(count)
                .build();
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        User currentUser = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        checkPermission(currentUser, review);

        reviewMapper.updateReview(review, request);
        review = reviewRepository.save(review);
        return reviewMapper.toReviewResponse(review);
    }

    @Override
    public void deleteReview(Long reviewId) {
        User currentUser = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        checkPermission(currentUser, review);

        reviewRepository.delete(review);
    }

    @Override
    public List<ReviewResponse> searchReviews(Long osmId, Integer minRating) {
        if (osmId != null && minRating != null) {
            return reviewRepository.findByOsmIdAndRatingGreaterThanEqual(osmId, minRating).stream()
                    .map(reviewMapper::toReviewResponse)
                    .toList();
        } else if (osmId != null) {
            return getReviewsByOsmId(osmId);
        } else {
            return reviewRepository.findAll().stream()
                    .map(reviewMapper::toReviewResponse)
                    .toList();
        }
    }
}
