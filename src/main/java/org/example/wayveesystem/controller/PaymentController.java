package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.response.PaymentLinkResponse;
import org.example.wayveesystem.dto.response.PaymentResponse;
import org.example.wayveesystem.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Management", description = "APIs for payOS payment gateway integration")
public class PaymentController {

    PaymentService paymentService;

    @Operation(summary = "Create payOS payment link for subscription")
    @PostMapping("/subscriptions/{subscriptionId}/pay")
    public ResponseEntity<ApiResponse<PaymentLinkResponse>> createPayment(
            @PathVariable("subscriptionId") Long subscriptionId) {
        PaymentLinkResponse response = paymentService.createPayment(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo link thanh toán thành công", response));
    }

    @Operation(summary = "Webhook endpoint for payOS payment status update")
    @PostMapping("/payos-webhook")
    public ResponseEntity<ApiResponse<Void>> handlePayOSWebhook(
            @RequestBody Webhook webhook) {
        paymentService.handlePayOSWebhook(webhook);
        return ResponseEntity.ok(ApiResponse.success("Xử lý webhook thành công"));
    }

    @Operation(summary = "Get payment details by paymentId")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable("paymentId") Long paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Cancel pending payment")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable("paymentId") Long paymentId) {
        PaymentResponse response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Hủy giao dịch thanh toán thành công", response));
    }

    @Operation(summary = "Get payment history of current user")
    @GetMapping("/my-payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PaymentResponse> response = paymentService.getMyPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
