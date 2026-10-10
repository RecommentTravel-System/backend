package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplanPreviewRequest {

    @NotNull(message = "Current day is required")
    @Min(value = 1, message = "Current day must be at least 1")
    Integer currentDay;

    Double currentLatitude;
    Double currentLongitude;
    LocalTime currentTime;

    List<Long> completedLocationIds;
    List<Long> skippedLocationIds;
    List<Long> postponedLocationIds;
}
