package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.PaymentMethod;
import org.example.wayveesystem.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {

    Long paymentId;
    Long subscriptionId;
    String planName;
    BigDecimal amount;
    PaymentMethod paymentMethod;
    String transactionCode;
    String paymentLinkId;
    PaymentStatus status;
    LocalDateTime paidAt;
    LocalDateTime createdAt;
}
