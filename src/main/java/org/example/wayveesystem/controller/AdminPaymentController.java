package org.example.wayveesystem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.PaymentStatus;
import org.example.wayveesystem.common.response.ApiResponse;
import org.example.wayveesystem.dto.response.PaymentResponse;
import org.example.wayveesystem.dto.response.PaymentSummaryResponse;
import org.example.wayveesystem.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Payment API", description = "Endpoints quản lý doanh thu và giao dịch thanh toán cho Admin")
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    PaymentService paymentService;

    @Operation(summary = "Lấy danh sách tất cả các giao dịch thanh toán (Admin)", description = "Lọc theo trạng thái và tìm kiếm mã đơn, khách hàng...")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(name = "status", required = false) PaymentStatus status,
            @RequestParam(name = "search", required = false) String search,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PaymentResponse> response = paymentService.getAllPaymentsForAdmin(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách giao dịch thành công", response));
    }

    @Operation(summary = "Lấy báo cáo tổng hợp doanh thu và trạng thái giao dịch chuyển tiền")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary() {
        PaymentSummaryResponse response = paymentService.getPaymentSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo doanh thu thành công", response));
    }

    @Operation(summary = "Đồng bộ trạng thái giao dịch với PayOS")
    @PostMapping("/{paymentId}/sync")
    public ResponseEntity<ApiResponse<PaymentResponse>> syncPayment(@PathVariable("paymentId") Long paymentId) {
        PaymentResponse response = paymentService.syncPaymentWithPayOS(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ trạng thái giao dịch thành công", response));
    }
}
