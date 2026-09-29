package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripRequest {

    @NotBlank(message = "TRIP_NAME_REQUIRED")
    @Size(max = 100, message = "TRIP_NAME_REQUIRED")
    String tripName;

    LocalDate startDate;
    LocalDate endDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "INVALID_KEY")
    BigDecimal budget;

    @NotNull(message = "INVALID_KEY")
    TripStatus status;

    /** Set true only after the user has confirmed the place order. */
    Boolean itineraryArranged;

    @AssertTrue(message = "INVALID_KEY")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
