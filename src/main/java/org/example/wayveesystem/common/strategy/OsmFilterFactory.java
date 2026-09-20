package org.example.wayveesystem.common.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OsmFilterFactory {

    private final List<OsmFilterStrategy> strategies;

    public List<String> buildFilterClauses(String filterKey, List<String> filterValues, double lat, double lng, int radiusMeters) {
        if (filterValues == null || filterValues.isEmpty()) {
            return List.of();
        }

        OsmFilterStrategy targetStrategy = strategies.stream()
                .filter(s -> s.supports(filterKey))
                .findFirst()
                .orElse(null);

        if (targetStrategy != null) {
            return targetStrategy.buildGroupedQueryClauses(filterValues, lat, lng, radiusMeters);
        }

        return List.of();
    }
}

