package org.example.wayveesystem.common.strategy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CategoryOsmFilterStrategy implements OsmFilterStrategy {

    private static final Map<String, List<String>> CATEGORY_OSM_MAP = Map.of(
            "RESTAURANT", List.of("amenity\"=\"restaurant", "amenity\"=\"food_court"),
            "CAFE", List.of("amenity\"=\"cafe", "amenity\"=\"bubble_tea"),
            "FAST_FOOD", List.of("amenity\"=\"fast_food"),
            "BAR", List.of("amenity\"=\"bar", "amenity\"=\"pub"),
            "BAKERY", List.of("shop\"=\"bakery", "craft\"=\"bakery"),
            "SHOPPING", List.of("shop\"=\"supermarket", "shop\"=\"convenience", "shop\"=\"mall"),
            "ATTRACTION", List.of("tourism\"=\"attraction", "tourism\"=\"museum", "leisure\"=\"park"),
            "ACCOMMODATION", List.of("tourism\"=\"hotel", "tourism\"=\"hostel", "tourism\"=\"guest_house")
    );

    @Override
    public boolean supports(String filterKey) {
        return "category".equalsIgnoreCase(filterKey);
    }

    @Override
    public List<String> buildQueryClauses(String filterValue, double lat, double lng, int radiusMeters) {
        List<String> clauses = new ArrayList<>();
        String normalizedValue = filterValue.toUpperCase();

        List<String> tags = CATEGORY_OSM_MAP.getOrDefault(normalizedValue, List.of("amenity\"=\"" + filterValue.toLowerCase()));

        for (String tag : tags) {
            clauses.add(String.format(java.util.Locale.US, "node[\"%s\"](around:%d,%.6f,%.6f);", tag, radiusMeters, lat, lng));
            clauses.add(String.format(java.util.Locale.US, "way[\"%s\"](around:%d,%.6f,%.6f);", tag, radiusMeters, lat, lng));
        }

        return clauses;
    }
}
