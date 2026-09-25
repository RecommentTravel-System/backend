package org.example.wayveesystem.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripResponse {
    Long tripId;
    String tripName;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal budget;
    TripStatus status;
    Boolean itineraryArranged;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
