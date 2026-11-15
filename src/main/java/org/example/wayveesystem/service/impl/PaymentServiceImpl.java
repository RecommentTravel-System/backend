package org.example.wayveesystem.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.enums.PaymentMethod;
import org.example.wayveesystem.common.enums.PaymentStatus;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.SubscriptionPlanType;
import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.common.exception.AppException;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.dto.response.PaymentLinkResponse;
import org.example.wayveesystem.dto.response.PaymentResponse;
import org.example.wayveesystem.dto.response.PaymentSummaryResponse;
import org.example.wayveesystem.model.Payment;
import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.PaymentRepository;
import org.example.wayveesystem.repository.SubscriptionRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentServiceImpl implements PaymentService {

    final PaymentRepository paymentRepository;
    final SubscriptionRepository subscriptionRepository;
    final UserRepository userRepository;
    final PayOS payOS;

    @Value("${payos.return-url}")
    String returnUrl;

    @Value("${payos.cancel-url}")
    String cancelUrl;

    @Value("${payos.env-code:1}")
    int envCode;

    private User getCurrentUser() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(Long.valueOf(userIdStr))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void checkPermission(User currentUser, Payment payment) {
        if (!payment.getSubscription().getUser().getUserId().equals(currentUser.getUserId())
                && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    @Override
    @Transactional
    public PaymentLinkResponse createPayment(Long subscriptionId) {
        User currentUser = getCurrentUser();

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        if (!subscription.getUser().getUserId().equals(currentUser.getUserId())
                && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }

        // Tạo bản ghi Payment với status PENDING
        Payment payment = Payment.builder()
                .subscription(subscription)
                .amount(subscription.getPrice())
                .paymentMethod(PaymentMethod.PAYOS)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        long epochSeconds = System.currentTimeMillis() / 1000;
        long paymentSuffix = payment.getPaymentId() % 10000;
        long orderCode = (long) Math.max(1, Math.min(8, envCode)) * 100_000_000_000_000L
                + (epochSeconds % 10_000_000_000L) * 10000L
                + paymentSuffix;

        payment.setOrderCode(orderCode);
        paymentRepository.save(payment);

        long amountInt = subscription.getPrice().intValue();
        String description =
                payment.getSubscription().getUser().getFullName() + " - " + payment.getSubscription().getPlanName();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amountInt)
                .description(description)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl)
                .build();

        CreatePaymentLinkResponse checkoutResponse;
        try {
            checkoutResponse = payOS.paymentRequests().create(paymentData);
        } catch (Exception e) {
            log.error("Failed to create payOS payment link for orderCode: {} (paymentId: {})", orderCode, payment.getPaymentId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
        }

        payment.setPaymentLinkId(checkoutResponse.getPaymentLinkId());
        paymentRepository.save(payment);

        return PaymentLinkResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderCode(payment.getOrderCode())
                .checkoutUrl(checkoutResponse.getCheckoutUrl())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .qrCode(checkoutResponse.getQrCode())
                .paymentLinkId(checkoutResponse.getPaymentLinkId())
                .build();
    }

    @Override
    @Transactional
    public void handlePayOSWebhook(Webhook webhook) {
        WebhookData webhookData;
        try {
            webhookData = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.PAYMENT_INVALID_SIGNATURE);
        }

        if (webhookData == null) {
            log.warn("Webhook data is null after verification");
            return;
        }

        long orderCode = webhookData.getOrderCode();
        // Tìm kiếm theo orderCode mới hoặc fallback theo paymentId (đối với các đơn cũ)
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseGet(() -> paymentRepository.findById(orderCode).orElse(null));

        // Trường hợp webhook test của payOS hoặc đơn thuộc môi trường khác (khi dùng chung account PayOS)
        if (payment == null) {
            log.warn("Payment record not found for orderCode: {} (likely PayOS test webhook or another environment)", orderCode);
            return;
        }

        // Đảm bảo tính Idempotent: nếu đã PAID thì bỏ qua
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment with orderCode: {} has already been PAID. Skipping duplicate webhook.", orderCode);
            return;
        }

        // Xử lý khi thanh toán thành công
        if ("00".equals(webhookData.getCode())) {
            BigDecimal webhookAmount = BigDecimal.valueOf(webhookData.getAmount());
            if (payment.getAmount().compareTo(webhookAmount) == 0) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
                payment.setTransactionCode(webhookData.getReference());
                paymentRepository.save(payment);

                // Kích hoạt Subscription
                Subscription subscription = payment.getSubscription();
                activateSubscription(subscription);

                log.info("Successfully activated subscription {} for payment {}",
                        subscription != null ? subscription.getSubscriptionId() : null, payment.getPaymentId());
            } else {
                log.error("Payment amount mismatch for orderCode {}: expected {}, received {}",
                        orderCode, payment.getAmount(), webhookAmount);
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        } else {
            log.warn("Payment failed for orderCode: {}, code: {}, desc: {}",
                    orderCode, webhookData.getCode(), webhookData.getDesc());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    @Override
    @Transactional
    public PaymentResponse getPaymentById(Long paymentId) {
        User currentUser = getCurrentUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        checkPermission(currentUser, payment);

        // Chủ động đồng bộ trạng thái với PayOS nếu đang PENDING (đề phòng webhook bị trễ hoặc chạy localhost)
        if (payment.getStatus() == PaymentStatus.PENDING) {
            try {
                long orderCode = payment.getOrderCode() != null ? payment.getOrderCode() : payment.getPaymentId();
                var paymentInfo = payOS.paymentRequests().get(orderCode);
                String payOsStatus = paymentInfo != null && paymentInfo.getStatus() != null ? paymentInfo.getStatus().name() : "UNKNOWN";
                log.info("PayOS sync check for paymentId {} (orderCode {}): PayOS status is '{}'", paymentId, orderCode, payOsStatus);

                if ("PAID".equalsIgnoreCase(payOsStatus)) {
                    log.info("PaymentId {} verified as PAID on PayOS. Updating database...", paymentId);
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    Subscription subscription = payment.getSubscription();
                    activateSubscription(subscription);
                    log.info("Successfully synchronized PAID status directly from PayOS for paymentId {}", paymentId);
                }
            } catch (Exception e) {
                log.warn("Could not sync PayOS status for paymentId {}: {}", paymentId, e.getMessage(), e);
            }
        }

        return toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        User currentUser = getCurrentUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        checkPermission(currentUser, payment);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new AppException(ErrorCode.PAYMENT_NOT_PENDING);
        }

        long orderCode = payment.getOrderCode() != null ? payment.getOrderCode() : payment.getPaymentId();
        try {
            payOS.paymentRequests().cancel(orderCode, "User requested cancellation");
        } catch (Exception e) {
            log.warn("Failed to cancel payment link on payOS for orderCode {}: {}", orderCode, e.getMessage());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);

        return toPaymentResponse(payment);
    }

    @Override
    public Page<PaymentResponse> getMyPayments(Pageable pageable) {
        User currentUser = getCurrentUser();
        return paymentRepository.findBySubscription_User(currentUser, pageable)
                .map(this::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPaymentsForAdmin(PaymentStatus status, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return paymentRepository.findAllForAdmin(status, cleanSearch, pageable)
                .map(this::toPaymentResponse);
    }

    @Override
    public PaymentSummaryResponse getPaymentSummary() {
        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        BigDecimal todayRevenue = paymentRepository.sumTotalRevenueFrom(startOfToday);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal monthRevenue = paymentRepository.sumTotalRevenueFrom(startOfMonth);
        if (monthRevenue == null) monthRevenue = BigDecimal.ZERO;

        BigDecimal failedAmount = paymentRepository.sumFailedOrCancelledAmount();
        if (failedAmount == null) failedAmount = BigDecimal.ZERO;

        long totalCount = paymentRepository.count();
        long successCount = paymentRepository.countByStatus(PaymentStatus.PAID)
                + paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long cancelledCount = paymentRepository.countByStatus(PaymentStatus.CANCELLED);
        long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);

        double successRate = totalCount > 0 ? ((double) successCount / totalCount) * 100.0 : 0.0;
        BigDecimal aov = successCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(successCount), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PaymentSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .failedAmount(failedAmount)
                .totalCount(totalCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .cancelledCount(cancelledCount)
                .pendingCount(pendingCount)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .averageOrderValue(aov)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse syncPaymentWithPayOS(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        try {
            long orderCode = payment.getOrderCode() != null ? payment.getOrderCode() : payment.getPaymentId();
            var paymentInfo = payOS.paymentRequests().get(orderCode);
            if (paymentInfo != null && paymentInfo.getStatus() != null) {
                String payOsStatus = paymentInfo.getStatus().name();
                log.info("Direct sync for paymentId {} (orderCode {}): PayOS status is '{}'", paymentId, orderCode, payOsStatus);

                if ("PAID".equalsIgnoreCase(payOsStatus)) {
                    payment.setStatus(PaymentStatus.PAID);
                    if (payment.getPaidAt() == null) {
                        payment.setPaidAt(LocalDateTime.now());
                    }
                    paymentRepository.save(payment);
                    activateSubscription(payment.getSubscription());
                } else if ("CANCELLED".equalsIgnoreCase(payOsStatus)) {
                    payment.setStatus(PaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                }
            }
        } catch (Exception e) {
            log.warn("Could not sync PayOS status for paymentId {}: {}", paymentId, e.getMessage());
        }

        return toPaymentResponse(payment);
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        User user = null;
        Subscription sub = payment.getSubscription();
        if (sub != null) {
            user = sub.getUser();
        }

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderCode(payment.getOrderCode() != null ? payment.getOrderCode() : payment.getPaymentId())
                .subscriptionId(sub != null ? sub.getSubscriptionId() : null)
                .planName(sub != null ? sub.getPlanName() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionCode(payment.getTransactionCode())
                .paymentLinkId(payment.getPaymentLinkId())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .userId(user != null ? user.getUserId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .userAvatarUrl(user != null ? user.getAvatarUrl() : null)
                .build();
    }

    private void activateSubscription(Subscription subscription) {
        if (subscription == null) return;
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        subscription.setStartDate(now);

        if (subscription.getPlanType() == SubscriptionPlanType.ANNUAL_PRO) {
            subscription.setEndDate(now.plusYears(1));
        } else {
            subscription.setEndDate(now.plusMonths(1));
        }

        int tripsCount = 1;
        String planName = subscription.getPlanName() != null ? subscription.getPlanName().toLowerCase() : "";
        if (planName.contains("10") || planName.contains("ten")) {
            tripsCount = 10;
        } else if (planName.contains("2") || planName.contains("duo")) {
            tripsCount = 2;
        } else if (subscription.getPrice() != null) {
            if (subscription.getPrice().compareTo(BigDecimal.valueOf(200000)) >= 0) {
                tripsCount = 10;
            } else if (subscription.getPrice().compareTo(BigDecimal.valueOf(50000)) >= 0) {
                tripsCount = 2;
            }
        }
        subscription.setTotalTrips(tripsCount);
        int currentRemaining = subscription.getRemainingTrips() != null ? subscription.getRemainingTrips() : 0;
        subscription.setRemainingTrips(Math.max(currentRemaining, tripsCount));
        subscriptionRepository.save(subscription);
    }
}
