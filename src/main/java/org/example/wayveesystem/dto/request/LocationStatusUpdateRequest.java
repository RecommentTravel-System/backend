package org.example.wayveesystem.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.wayveesystem.common.enums.TripLocationStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocationStatusUpdateRequest {

    TripLocationStatus status;
    Double currentLatitude;
    Double currentLongitude;
    LocalDateTime visitedAt;
}
