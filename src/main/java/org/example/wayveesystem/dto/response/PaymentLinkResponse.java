package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.PaymentStatus;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentLinkResponse {

    Long paymentId;
    String checkoutUrl;
    BigDecimal amount;
    PaymentStatus status;
    String qrCode;
    String paymentLinkId;
}
