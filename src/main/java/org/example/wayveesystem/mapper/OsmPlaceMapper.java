package org.example.wayveesystem.mapper;

import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.response.OverpassElementResponse;
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

    /**
     * Maps an Overpass API element to an in-memory OsmPlace POJO.
     * Extracts a direct image URL from the OSM image tag when present.
     */
    public OsmPlace toOsmPlace(OverpassElementResponse el) {
        if (el == null || el.tags() == null) {
            return null;
        }

        Map<String, String> tags = el.tags();
        String rawTag = firstMatchingTag(tags);
        String categoryCode = TAG_TO_CATEGORY.getOrDefault(rawTag, "OTHER");
        String name = tags.getOrDefault("name", "Địa điểm OSM #" + el.id());
        String address = extractAddress(tags);
        String imageUrl = validImageUrl(tags.get("image"));
        ImageSource imageSource = imageUrl == null ? null : ImageSource.OSM;
        String openingHours = tags.get("opening_hours");
        String phone = tags.getOrDefault("phone", tags.get("contact:phone"));
        String website = tags.getOrDefault("website", tags.get("contact:website"));

        return new OsmPlace(
                el.id(),
                name,
                categoryCode,
                address,
                el.getLat(),
                el.getLon(),
                imageUrl,
                imageSource,
                openingHours,
                phone,
                website,
                tags
        );
    }

    private String validImageUrl(String candidate) {
        if (candidate == null || candidate.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(candidate.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null ? uri.toString() : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
