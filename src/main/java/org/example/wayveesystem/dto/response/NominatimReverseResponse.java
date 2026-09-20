package org.example.wayveesystem.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimReverseResponse {

    @JsonProperty("place_id")
    Long placeId;

    String licence;

    @JsonProperty("osm_type")
    String osmType;

    @JsonProperty("osm_id")
    Long osmId;

    String lat;

    String lon;

    @JsonProperty("display_name")
    String displayName;

    String name;

    Map<String, Object> address;

    @JsonProperty("boundingbox")
    String[] boundingBox;
}
