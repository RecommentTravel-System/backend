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

        long orderCode = payment.getPaymentId();
        long amountInt = subscription.getPrice().intValue();
        String description = "Thanh toan " + orderCode;

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
            log.error("Failed to create payOS payment link for paymentId: {}", orderCode, e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
        }

        payment.setPaymentLinkId(checkoutResponse.getPaymentLinkId());
        paymentRepository.save(payment);

        return PaymentLinkResponse.builder()
                .paymentId(payment.getPaymentId())
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
        Payment payment = paymentRepository.findById(orderCode).orElse(null);

        // Trường hợp webhook test của payOS hoặc không tìm thấy orderCode
        if (payment == null) {
            log.warn("Payment record not found for orderCode: {} (likely a PayOS test webhook)", orderCode);
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
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                LocalDateTime now = LocalDateTime.now();
                subscription.setStartDate(now);

                if (subscription.getPlanType() == SubscriptionPlanType.ANNUAL_PRO) {
                    subscription.setEndDate(now.plusYears(1));
                } else {
                    subscription.setEndDate(now.plusMonths(1));
                }
                subscriptionRepository.save(subscription);

                log.info("Successfully activated subscription {} for payment {}",
                        subscription.getSubscriptionId(), payment.getPaymentId());
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
                var paymentInfo = payOS.paymentRequests().get(payment.getPaymentId());
                if (paymentInfo != null && "PAID".equalsIgnoreCase(paymentInfo.getStatus().name())) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);

                    Subscription subscription = payment.getSubscription();
                    if (subscription != null) {
                        subscription.setStatus(SubscriptionStatus.ACTIVE);
                        LocalDateTime now = LocalDateTime.now();
                        subscription.setStartDate(now);
                        if (subscription.getPlanType() == SubscriptionPlanType.ANNUAL_PRO) {
                            subscription.setEndDate(now.plusYears(1));
                        } else {
                            subscription.setEndDate(now.plusMonths(1));
                        }
                        subscriptionRepository.save(subscription);
                    }
                    log.info("Synchronized PAID status directly from PayOS for paymentId {}", paymentId);
                }
            } catch (Exception e) {
                log.debug("Could not sync PayOS status for paymentId {}: {}", paymentId, e.getMessage());
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

        try {
            payOS.paymentRequests().cancel(payment.getPaymentId(), "User requested cancellation");
        } catch (Exception e) {
            log.warn("Failed to cancel payment link on payOS for orderCode {}: {}", payment.getPaymentId(), e.getMessage());
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

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .subscriptionId(payment.getSubscription() != null ? payment.getSubscription().getSubscriptionId() : null)
                .planName(payment.getSubscription() != null ? payment.getSubscription().getPlanName() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionCode(payment.getTransactionCode())
                .paymentLinkId(payment.getPaymentLinkId())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
