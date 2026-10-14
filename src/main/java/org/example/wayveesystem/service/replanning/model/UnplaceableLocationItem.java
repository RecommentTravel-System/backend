package org.example.wayveesystem.service.replanning.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.LocationPriority;
import org.example.wayveesystem.common.enums.ReplanAction;
import org.example.wayveesystem.common.enums.ReplanSeverity;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnplaceableLocationItem {
    Long tripLocationId;
    Long osmId;
    String placeName;
    LocationPriority priority;
    Integer originalDay;
    String reason;
    ReplanSeverity severity;
    @Builder.Default
    List<ReplanAction> suggestedActions = new ArrayList<>();
}
