package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSummaryResponse {
    BigDecimal totalRevenue;
    BigDecimal todayRevenue;
    BigDecimal monthRevenue;
    BigDecimal failedAmount;
    long totalCount;
    long successCount;
    long failedCount;
    long cancelledCount;
    long pendingCount;
    double successRate;
    BigDecimal averageOrderValue;
}
