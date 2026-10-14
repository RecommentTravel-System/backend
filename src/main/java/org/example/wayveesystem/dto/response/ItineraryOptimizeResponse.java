package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.dto.request.OptimizePlaceItemDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItineraryOptimizeResponse {
    @Builder.Default
    List<OptimizedDayDto> days = new ArrayList<>();
    @Builder.Default
    List<OptimizePlaceItemDto> unassignedPlaces = new ArrayList<>();
    Double totalDistanceKm;
    Integer totalTravelMinutes;
}
