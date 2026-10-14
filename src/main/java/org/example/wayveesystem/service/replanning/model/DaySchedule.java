package org.example.wayveesystem.service.replanning.model;

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
public class DaySchedule {
    Integer day;
    LocalDate date;
    LocalTime startTime;
    LocalTime maxEndTime;
    Double startLatitude;
    Double startLongitude;
    @Builder.Default
    List<ReplanLocationItem> locations = new ArrayList<>();

    public DaySchedule deepCopy() {
        List<ReplanLocationItem> copyLocations = new ArrayList<>();
        if (locations != null) {
            for (ReplanLocationItem loc : locations) {
                copyLocations.add(ReplanLocationItem.copyOf(loc));
            }
        }
        return DaySchedule.builder()
                .day(day)
                .date(date)
                .startTime(startTime)
                .maxEndTime(maxEndTime)
                .startLatitude(startLatitude)
                .startLongitude(startLongitude)
                .locations(copyLocations)
                .build();
    }
}
