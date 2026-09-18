package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.ReviewRequest;
import org.example.wayveesystem.dto.request.ReviewUpdateRequest;
import org.example.wayveesystem.dto.response.ReviewResponse;
import org.example.wayveesystem.dto.response.ReviewSummaryResponse;
import org.example.wayveesystem.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Review API", description = "Endpoints đánh giá địa điểm (Review CRUDS)")
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {

    ReviewService reviewService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Tạo đánh giá mới", description = "Tạo một đánh giá và số sao (1-5) cho địa điểm OSM ID")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", response));
    }

    @Operation(summary = "Lấy danh sách đánh giá theo địa điểm (OSM ID)", description = "Lấy tất cả các đánh giá dành cho một địa điểm cụ thể")
    @GetMapping("/place/{osmId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByOsmId(@PathVariable("osmId") Long osmId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByOsmId(osmId);
        return ResponseEntity.ok(ApiResponse.success("Get reviews by location successful", reviews));
    }

    @Operation(summary = "Lấy tổng quan đánh giá của địa điểm", description = "Lấy điểm trung bình (Rating Average) và tổng số bài đánh giá cho OSM ID")
    @GetMapping("/place/{osmId}/summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummaryByOsmId(@PathVariable("osmId") Long osmId) {
        ReviewSummaryResponse summary = reviewService.getReviewSummaryByOsmId(osmId);
        return ResponseEntity.ok(ApiResponse.success("Get review summary successful", summary));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy danh sách đánh giá của tôi", description = "Lấy danh sách bài đánh giá mà người dùng hiện tại đã viết")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews() {
        List<ReviewResponse> reviews = reviewService.getMyReviews();
        return ResponseEntity.ok(ApiResponse.success("Get my reviews successful", reviews));
    }

    @Operation(summary = "Lấy chi tiết bài đánh giá theo ID", description = "Lấy thông tin chi tiết bài đánh giá theo Review ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable("id") Long reviewId) {
        ReviewResponse response = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Get review details successful", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cập nhật bài đánh giá", description = "Chỉnh sửa bài đánh giá (Chỉ tác giả hoặc Admin)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable("id") Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        ReviewResponse response = reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Xóa bài đánh giá", description = "Xóa bài đánh giá (Chỉ tác giả hoặc Admin)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable("id") Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
    }

    @Operation(summary = "Tìm kiếm / Lọc đánh giá", description = "Tìm kiếm bài đánh giá theo OSM ID và số sao tối thiểu (minRating)")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> searchReviews(
            @RequestParam(name = "osmId", required = false) Long osmId,
            @RequestParam(name = "minRating", required = false) Integer minRating) {
        List<ReviewResponse> responses = reviewService.searchReviews(osmId, minRating);
        return ResponseEntity.ok(ApiResponse.success("Search reviews successful", responses));
    }
}
