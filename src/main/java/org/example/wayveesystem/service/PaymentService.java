package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.response.PaymentLinkResponse;
import org.example.wayveesystem.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.payos.model.webhooks.Webhook;

/**
 * Service interface quản lý các nghiệp vụ thanh toán (Payment) qua cổng payOS.
 */
public interface PaymentService {

    /**
     * Khởi tạo giao dịch thanh toán cho một gói Subscription và tạo link thanh toán payOS.
     *
     * @param subscriptionId ID của subscription cần thanh toán
     * @return PaymentLinkResponse chứa thông tin thanh toán và link checkout của payOS
     */
    PaymentLinkResponse createPayment(Long subscriptionId);

    /**
     * Xử lý webhook bất đồng bộ gửi từ cổng thanh toán payOS khi trạng thái giao dịch thay đổi.
     *
     * @param webhook Dữ liệu webhook chứa thông tin giao dịch và chữ ký số từ payOS
     */
    void handlePayOSWebhook(Webhook webhook);

    /**
     * Lấy thông tin chi tiết của một giao dịch thanh toán theo paymentId.
     * Chỉ chủ sở hữu giao dịch hoặc ADMIN mới có quyền xem.
     *
     * @param paymentId ID của bản ghi Payment
     * @return PaymentResponse thông tin chi tiết giao dịch
     */
    PaymentResponse getPaymentById(Long paymentId);

    /**
     * Hủy giao dịch thanh toán đang ở trạng thái PENDING và hủy link thanh toán bên payOS.
     *
     * @param paymentId ID của bản ghi Payment cần hủy
     * @return PaymentResponse thông tin giao dịch sau khi hủy
     */
    PaymentResponse cancelPayment(Long paymentId);

    /**
     * Lấy danh sách lịch sử các giao dịch thanh toán của người dùng hiện tại (có phân trang).
     *
     * @param pageable Thông tin phân trang và sắp xếp
     * @return Page<PaymentResponse> danh sách giao dịch
     */
    Page<PaymentResponse> getMyPayments(Pageable pageable);
}
