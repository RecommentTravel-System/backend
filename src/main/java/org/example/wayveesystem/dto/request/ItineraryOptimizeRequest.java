package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItineraryOptimizeRequest {

    @Min(value = 1, message = "Total days must be at least 1")
    @Builder.Default
    Integer totalDays = 1;

    LocalDate startDate;

    LocalTime dailyStartTime;

    @Builder.Default
    List<OptimizePlaceItemDto> places = new ArrayList<>();
}
