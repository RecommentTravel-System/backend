package org.example.wayveesystem.service.replanning.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.ReplanSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InsertionPlanResult {
    @Builder.Default
    List<DaySchedule> schedules = new ArrayList<>();
    @Builder.Default
    List<UnplaceableLocationItem> unplaceableLocations = new ArrayList<>();
    @Builder.Default
    Set<Integer> modifiedDays = new HashSet<>();
    @Builder.Default
    ReplanSeverity severity = ReplanSeverity.NORMAL;
}
