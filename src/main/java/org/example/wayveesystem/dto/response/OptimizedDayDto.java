package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.dto.request.OptimizePlaceItemDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OptimizedDayDto {
    Integer day;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    Double totalDistanceKm;
    Integer totalTravelMinutes;
    @Builder.Default
    List<OptimizePlaceItemDto> items = new ArrayList<>();
}
