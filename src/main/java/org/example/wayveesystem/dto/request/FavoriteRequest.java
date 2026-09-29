package org.example.wayveesystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FavoriteRequest {

    @NotNull(message = "OSM ID is required")
    Long osmId;

    String placeName;

    Double latitude;

    Double longitude;
}
