package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SubscriptionRequest {

    @NotBlank(message = "Plan name is required")
    String planName;

    @NotNull(message = "Plan type is required")
    SubscriptionPlanType planType;

    BigDecimal price;

    LocalDateTime startDate;

    LocalDateTime endDate;

    SubscriptionStatus status;
}
