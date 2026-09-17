package org.example.wayveesystem.mapper;

import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.response.OverpassElementResponse;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${wayvee.poi.default-image-url}")
    private String defaultImageUrl;

    /**
     * Maps an Overpass API element to an in-memory OsmPlace POJO.
     * Extracts image URLs from OSM tags (image, wikimedia_commons).
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
        String imageUrl = extractImageUrl(tags);
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
                openingHours,
                phone,
                website,
                tags
        );
    }

    /**
     * Extracts image URL from OSM tags.
     * Priority: image tag > wikimedia_commons tag > default image.
     */
    private String extractImageUrl(Map<String, String> tags) {
        // Direct image URL from OSM
        String imageTag = tags.get("image");
        if (imageTag != null && !imageTag.isBlank()) {
            return imageTag;
        }

        // Wikimedia Commons: build thumbnail URL
        String wikimediaCommons = tags.get("wikimedia_commons");
        if (wikimediaCommons != null && !wikimediaCommons.isBlank()) {
            // Format: "File:Example.jpg" → thumbnail URL
            String fileName = wikimediaCommons.startsWith("File:")
                    ? wikimediaCommons.substring(5)
                    : wikimediaCommons;
            return "https://commons.wikimedia.org/wiki/Special:FilePath/"
                    + fileName.replace(" ", "_") + "?width=400";
        }

        // Wikidata: could resolve but would require extra API call, use default instead
        return defaultImageUrl;
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
