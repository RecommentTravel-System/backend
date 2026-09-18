package org.example.wayveesystem.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FavoriteResponse {

    Long favoriteId;
    Long userId;
    Long osmId;
    String placeName;
    Double latitude;
    Double longitude;
    LocalDateTime createdAt;
}
