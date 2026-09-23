package org.example.wayveesystem.common.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CuisineOsmFilterStrategy implements OsmFilterStrategy {

    @Override
    public boolean supports(String filterKey) {
        return "cuisine".equalsIgnoreCase(filterKey);
    }

    @Override
    public List<String> buildQueryClauses(String filterValue, double lat, double lng, int radiusMeters) {
        String tag = "cuisine\"=\"" + filterValue.toLowerCase();
        return List.of(
                String.format(java.util.Locale.US, "node[\"%s\"](around:%d,%.6f,%.6f);", tag, radiusMeters, lat, lng),
                String.format(java.util.Locale.US, "way[\"%s\"](around:%d,%.6f,%.6f);", tag, radiusMeters, lat, lng),
                String.format(java.util.Locale.US, "relation[\"%s\"](around:%d,%.6f,%.6f);", tag, radiusMeters, lat, lng)
        );
    }
}
