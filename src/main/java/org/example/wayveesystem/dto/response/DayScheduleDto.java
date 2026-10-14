package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DayScheduleDto {
    Integer day;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    Double totalDistanceKm;
    Integer totalTravelMinutes;
    Integer totalStayMinutes;
    List<ReplanItemDto> locations;
}
