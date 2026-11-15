package org.example.wayveesystem.service;

import org.example.wayveesystem.common.enums.PaymentMethod;
import org.example.wayveesystem.common.enums.PaymentStatus;
import org.example.wayveesystem.common.enums.Role;
import org.example.wayveesystem.common.enums.SubscriptionStatus;
import org.example.wayveesystem.dto.response.PaymentLinkResponse;
import org.example.wayveesystem.model.Payment;
import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.model.User;
import org.example.wayveesystem.repository.PaymentRepository;
import org.example.wayveesystem.repository.SubscriptionRepository;
import org.example.wayveesystem.repository.UserRepository;
import org.example.wayveesystem.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServicePayOsTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PayOS payOS;

    @Mock
    private PaymentRequestsService paymentRequests;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "returnUrl", "https://wayvee.me/payment/success");
        ReflectionTestUtils.setField(paymentService, "cancelUrl", "https://wayvee.me/payment/cancel");
        ReflectionTestUtils.setField(paymentService, "envCode", 1);

        currentUser = User.builder()
                .userId(1L)
                .email("traveler@gmail.com")
                .fullName("Nguyễn Hoàng Gia Bảo")
                .role(Role.USER)
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("1");
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
    }

    @Test
    void createPayment_WithAllThreePlans_DescriptionNeverExceeds25Characters() throws Exception {
        String[] plans = {
                "1 chuyến đi",
                "Combo 2 chuyến đi",
                "Combo 10 chuyến đi"
        };
        BigDecimal[] prices = {
                BigDecimal.valueOf(28000),
                BigDecimal.valueOf(70000),
                BigDecimal.valueOf(350000)
        };

        when(payOS.paymentRequests()).thenReturn(paymentRequests);

        for (int i = 0; i < plans.length; i++) {
            Long subId = (long) (i + 1);
            Subscription subscription = Subscription.builder()
                    .subscriptionId(subId)
                    .user(currentUser)
                    .planName(plans[i])
                    .price(prices[i])
                    .status(SubscriptionStatus.PENDING)
                    .build();

            when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(subscription));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                if (p.getPaymentId() == null) {
                    p.setPaymentId(100L + subId);
                }
                return p;
            });

            CreatePaymentLinkResponse mockResponse = new CreatePaymentLinkResponse();
            mockResponse.setCheckoutUrl("https://pay.payos.vn/web/12345");
            mockResponse.setPaymentLinkId("link_" + subId);
            mockResponse.setQrCode("000201010212...");

            ArgumentCaptor<CreatePaymentLinkRequest> captor = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
            when(paymentRequests.create(captor.capture())).thenReturn(mockResponse);

            PaymentLinkResponse response = paymentService.createPayment(subId);

            assertNotNull(response);
            assertEquals("https://pay.payos.vn/web/12345", response.getCheckoutUrl());

            CreatePaymentLinkRequest sentRequest = captor.getValue();
            assertNotNull(sentRequest.getDescription());
            assertTrue(sentRequest.getDescription().length() <= 25,
                    "Description for plan '" + plans[i] + "' exceeded 25 chars: '" + sentRequest.getDescription() + "' (length: " + sentRequest.getDescription().length() + ")");
            assertFalse(sentRequest.getDescription().matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ].*"),
                    "Description should not contain Vietnamese accented characters: " + sentRequest.getDescription());
        }
    }
}
