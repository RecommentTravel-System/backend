package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MovedLocationDto {
    Long tripLocationId;
    Long osmId;
    String placeName;
    Integer fromDay;
    Integer toDay;
    Integer fromOrder;
    Integer toOrder;
    String reason;
}
