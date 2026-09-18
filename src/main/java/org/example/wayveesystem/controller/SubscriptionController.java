package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.SubscriptionPlanType;
import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.request.SubscriptionRequest;
import org.example.wayveesystem.dto.request.SubscriptionUpdateRequest;
import org.example.wayveesystem.dto.response.SubscriptionResponse;
import org.example.wayveesystem.service.SubscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Subscription API", description = "Endpoints quản lý Gói dịch vụ đăng ký (Subscription CRUDS)")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {

    SubscriptionService subscriptionService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Đăng ký gói dịch vụ mới", description = "Tạo một đăng ký gói dịch vụ cho người dùng hiện tại")
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(@Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription created successfully", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy gói đăng ký đang hoạt động của tôi", description = "Lấy thông tin gói dịch vụ ACTIVE mới nhất của người dùng đang đăng nhập")
    @GetMapping("/me/active")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMyActiveSubscription() {
        SubscriptionResponse response = subscriptionService.getMyActiveSubscription();
        return ResponseEntity.ok(ApiResponse.success("Get active subscription successful", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy danh sách gói đăng ký của tôi", description = "Lấy tất cả lịch sử các gói dịch vụ đã đăng ký của tôi")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptions() {
        List<SubscriptionResponse> subscriptions = subscriptionService.getMySubscriptions();
        return ResponseEntity.ok(ApiResponse.success("Get my subscriptions successful", subscriptions));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy tất cả gói đăng ký (Admin)", description = "Lấy tất cả gói dịch vụ đăng ký trong hệ thống")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getAllSubscriptions() {
        List<SubscriptionResponse> subscriptions = subscriptionService.getAllSubscriptions();
        return ResponseEntity.ok(ApiResponse.success("Get all subscriptions successful", subscriptions));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Lấy chi tiết gói đăng ký theo ID", description = "Lấy chi tiết một thông tin đăng ký theo Subscription ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscriptionById(@PathVariable("id") Long subscriptionId) {
        SubscriptionResponse response = subscriptionService.getSubscriptionById(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success("Get subscription details successful", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật gói đăng ký (Admin)", description = "Cập nhật thông tin gói đăng ký theo ID")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscription(
            @PathVariable("id") Long subscriptionId,
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        SubscriptionResponse response = subscriptionService.updateSubscription(subscriptionId, request);
        return ResponseEntity.ok(ApiResponse.success("Subscription updated successfully", response));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Hủy gói đăng ký", description = "Hủy gói đăng ký hiện tại (chuyển status thành CANCELLED)")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(@PathVariable("id") Long subscriptionId) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled successfully", response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa gói đăng ký (Admin)", description = "Xóa một bản ghi gói đăng ký theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubscription(@PathVariable("id") Long subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success("Subscription deleted successfully"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tìm kiếm / Lọc gói đăng ký (Admin)", description = "Lọc các gói đăng ký theo status và planType")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> searchSubscriptions(
            @RequestParam(name = "status", required = false) SubscriptionStatus status,
            @RequestParam(name = "planType", required = false) SubscriptionPlanType planType) {
        List<SubscriptionResponse> responses = subscriptionService.searchSubscriptions(status, planType);
        return ResponseEntity.ok(ApiResponse.success("Search subscriptions successful", responses));
    }
}
