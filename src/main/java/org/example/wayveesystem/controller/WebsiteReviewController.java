package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.WebsiteReviewReplyRequest;
import org.example.wayveesystem.dto.request.WebsiteReviewRequest;
import org.example.wayveesystem.dto.response.ImageUploadResponse;
import org.example.wayveesystem.dto.response.WebsiteReviewResponse;
import org.example.wayveesystem.dto.response.WebsiteReviewStatsResponse;
import org.example.wayveesystem.service.WebsiteReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Website Review API", description = "Endpoints đánh giá trang web và phản hồi từ Admin")
@RestController
@RequestMapping("/api/v1/website-reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*")
public class WebsiteReviewController {

    WebsiteReviewService websiteReviewService;

    @Operation(summary = "Gửi đánh giá trang web", description = "Người dùng gửi đánh giá, góp ý cho trang web Wayvee (số sao 1-5, khía cạnh, nội dung và ảnh/video đính kèm)")
    @PostMapping
    public ResponseEntity<ApiResponse<WebsiteReviewResponse>> submitReview(@Valid @RequestBody WebsiteReviewRequest request) {
        WebsiteReviewResponse response = websiteReviewService.createWebsiteReview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đánh giá trang web đã được gửi thành công", response));
    }

    @Operation(summary = "Tải lên ảnh hoặc video cho feedback", description = "Tải lên tệp ảnh hoặc video đính kèm cho đánh giá trang web lên Cloudinary")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadMedia(@RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = websiteReviewService.uploadMedia(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Tải tệp đa phương tiện thành công", response));
    }

    @Operation(summary = "Admin phản hồi đánh giá trang web", description = "Quản trị viên phản hồi trực tiếp tới đánh giá/góp ý của người dùng")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<WebsiteReviewResponse>> replyReview(
            @PathVariable("id") Long id,
            @Valid @RequestBody WebsiteReviewReplyRequest request) {
        WebsiteReviewResponse response = websiteReviewService.replyWebsiteReview(id, request.getReply());
        return ResponseEntity.ok(ApiResponse.success("Phản hồi đánh giá thành công", response));
    }

    @Operation(summary = "Admin xóa phản hồi đánh giá trang web", description = "Quản trị viên xóa nội dung phản hồi đã gửi")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<WebsiteReviewResponse>> deleteReply(@PathVariable("id") Long id) {
        WebsiteReviewResponse response = websiteReviewService.deleteReply(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa phản hồi thành công", response));
    }

    @Operation(summary = "Lấy danh sách đánh giá trang web", description = "Lấy danh sách các đánh giá trang web để hiển thị cho quản trị viên và người dùng")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WebsiteReviewResponse>>> getAllReviews(
            @RequestParam(name = "rating", required = false) Integer rating,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "search", required = false) String search) {
        List<WebsiteReviewResponse> list = websiteReviewService.getAllWebsiteReviews(rating, category, search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá trang web thành công", list));
    }

    @Operation(summary = "Thống kê đánh giá trang web", description = "Trả về tổng số đánh giá, điểm trung bình và phân bố sao")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<WebsiteReviewStatsResponse>> getStats() {
        WebsiteReviewStatsResponse stats = websiteReviewService.getWebsiteReviewStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê đánh giá trang web thành công", stats));
    }

    @Operation(summary = "Lấy đánh giá trang web của tôi", description = "Lấy các đánh giá trang web do người dùng hiện tại gửi")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<WebsiteReviewResponse>>> getMyReviews() {
        List<WebsiteReviewResponse> list = websiteReviewService.getMyWebsiteReviews();
        return ResponseEntity.ok(ApiResponse.success("Lấy đánh giá cá nhân thành công", list));
    }
}

