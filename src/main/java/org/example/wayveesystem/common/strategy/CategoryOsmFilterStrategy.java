package org.example.wayveesystem.common.strategy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CategoryOsmFilterStrategy implements OsmFilterStrategy {

    public record TagPair(String key, String value) {}

    private static final Map<String, List<TagPair>> CATEGORY_TAG_MAP = Map.of(
            "RESTAURANT", List.of(new TagPair("amenity", "restaurant"), new TagPair("amenity", "food_court")),
            "CAFE", List.of(new TagPair("amenity", "cafe"), new TagPair("amenity", "bubble_tea")),
            "FAST_FOOD", List.of(new TagPair("amenity", "fast_food")),
            "BAR", List.of(new TagPair("amenity", "bar"), new TagPair("amenity", "pub")),
            "BAKERY", List.of(new TagPair("shop", "bakery"), new TagPair("craft", "bakery")),
            "SHOPPING", List.of(new TagPair("shop", "supermarket"), new TagPair("shop", "convenience"), new TagPair("shop", "mall")),
            "ATTRACTION", List.of(new TagPair("tourism", "attraction"), new TagPair("tourism", "museum"), new TagPair("leisure", "park")),
            "ACCOMMODATION", List.of(new TagPair("tourism", "hotel"), new TagPair("tourism", "hostel"), new TagPair("tourism", "guest_house"))
    );

    @Override
    public boolean supports(String filterKey) {
        return "category".equalsIgnoreCase(filterKey);
    }

    @Override
    public List<String> buildQueryClauses(String filterValue, double lat, double lng, int radiusMeters) {
        return buildGroupedQueryClauses(List.of(filterValue), lat, lng, radiusMeters);
    }

    @Override
    public List<String> buildGroupedQueryClauses(List<String> filterValues, double lat, double lng, int radiusMeters) {
        if (filterValues == null || filterValues.isEmpty()) {
            return List.of();
        }

        // Map tag key -> Set of unique tag values
        Map<String, Set<String>> groupedTags = new LinkedHashMap<>();

        for (String cat : filterValues) {
            String normalizedCat = cat.toUpperCase();
            List<TagPair> pairs = CATEGORY_TAG_MAP.getOrDefault(
                    normalizedCat,
                    List.of(new TagPair("amenity", cat.toLowerCase()))
            );
            for (TagPair pair : pairs) {
                groupedTags.computeIfAbsent(pair.key(), k -> new LinkedHashSet<>()).add(pair.value());
            }
        }

        List<String> clauses = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : groupedTags.entrySet()) {
            String tagKey = entry.getKey();
            Set<String> values = entry.getValue();

            if (values.size() == 1) {
                String singleVal = values.iterator().next();
                clauses.add(String.format(java.util.Locale.US, "node[\"%s\"=\"%s\"](around:%d,%.6f,%.6f);", tagKey, singleVal, radiusMeters, lat, lng));
                clauses.add(String.format(java.util.Locale.US, "way[\"%s\"=\"%s\"](around:%d,%.6f,%.6f);", tagKey, singleVal, radiusMeters, lat, lng));
                clauses.add(String.format(java.util.Locale.US, "relation[\"%s\"=\"%s\"](around:%d,%.6f,%.6f);", tagKey, singleVal, radiusMeters, lat, lng));
            } else {
                String regexVal = values.stream().sorted().collect(Collectors.joining("|"));
                clauses.add(String.format(java.util.Locale.US, "node[\"%s\"~\"^(%s)$\"](around:%d,%.6f,%.6f);", tagKey, regexVal, radiusMeters, lat, lng));
                clauses.add(String.format(java.util.Locale.US, "way[\"%s\"~\"^(%s)$\"](around:%d,%.6f,%.6f);", tagKey, regexVal, radiusMeters, lat, lng));
                clauses.add(String.format(java.util.Locale.US, "relation[\"%s\"~\"^(%s)$\"](around:%d,%.6f,%.6f);", tagKey, regexVal, radiusMeters, lat, lng));
            }
        }

        return clauses;
    }
}
