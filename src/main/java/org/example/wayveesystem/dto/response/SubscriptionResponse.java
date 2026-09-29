package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.SubscriptionPlanType;
import org.example.wayveesystem.common.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionResponse {

    Long subscriptionId;
    Long userId;
    String userEmail;
    String userFullName;
    String planName;
    SubscriptionPlanType planType;
    BigDecimal price;
    LocalDateTime startDate;
    LocalDateTime endDate;
    SubscriptionStatus status;
    LocalDateTime createdAt;
}
