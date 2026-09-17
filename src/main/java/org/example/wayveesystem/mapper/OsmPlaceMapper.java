package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.response.OverpassElementResponse;
import org.example.wayveesystem.model.Location;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OsmPlaceMapper {

    private static final Map<String, String> TAG_TO_CATEGORY = Map.ofEntries(
            Map.entry("restaurant", "RESTAURANT"),
            Map.entry("cafe", "CAFE"),
            Map.entry("fast_food", "FAST_FOOD"),
            Map.entry("bar", "BAR"),
            Map.entry("pub", "BAR"),
            Map.entry("bakery", "BAKERY"),
            Map.entry("supermarket", "SHOPPING"),
            Map.entry("convenience", "SHOPPING"),
            Map.entry("mall", "SHOPPING"),
            Map.entry("attraction", "ATTRACTION"),
            Map.entry("museum", "ATTRACTION"),
            Map.entry("hotel", "ACCOMMODATION"),
            Map.entry("hostel", "ACCOMMODATION")
    );

    public Location toLocation(OverpassElementResponse el) {
        if (el == null || el.tags() == null) {
            return null;
        }

        Map<String, String> tags = el.tags();
        String rawTag = firstMatchingTag(tags);
        String categoryCode = TAG_TO_CATEGORY.getOrDefault(rawTag, "OTHER");
        String name = tags.getOrDefault("name", "Địa điểm OSM #" + el.id());
        String address = extractAddress(tags);

        return Location.builder()
                .sourceOsmId(el.id())
                .name(name)
                .address(address)
                .latitude(el.getLat())
                .longitude(el.getLon())
                .categoryCode(categoryCode)
                .rating(0.0)
                .reviewCount(0)
                .build();
    }

    private String firstMatchingTag(Map<String, String> tags) {
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            if (TAG_TO_CATEGORY.containsKey(entry.getValue())) {
                return entry.getValue();
            }
            if (TAG_TO_CATEGORY.containsKey(entry.getKey())) {
                return entry.getKey();
            }
        }
        return "other";
    }

    private String extractAddress(Map<String, String> tags) {
        StringBuilder addressBuilder = new StringBuilder();

        if (tags.containsKey("addr:housenumber")) {
            addressBuilder.append(tags.get("addr:housenumber")).append(" ");
        }
        if (tags.containsKey("addr:street")) {
            addressBuilder.append(tags.get("addr:street")).append(", ");
        }
        if (tags.containsKey("addr:district") || tags.containsKey("addr:suburb")) {
            String district = tags.getOrDefault("addr:district", tags.get("addr:suburb"));
            addressBuilder.append(district).append(", ");
        }
        if (tags.containsKey("addr:city")) {
            addressBuilder.append(tags.get("addr:city"));
        }

        String address = addressBuilder.toString().trim();
        if (address.endsWith(",")) {
            address = address.substring(0, address.length() - 1);
        }
        return address.isEmpty() ? "Đang cập nhật địa chỉ" : address;
    }
}
