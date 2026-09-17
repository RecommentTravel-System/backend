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
        List<String> clauses = new ArrayList<>();

        OsmFilterStrategy targetStrategy = strategies.stream()
                .filter(s -> s.supports(filterKey))
                .findFirst()
                .orElse(null);

        if (targetStrategy != null && filterValues != null) {
            for (String val : filterValues) {
                clauses.addAll(targetStrategy.buildQueryClauses(val, lat, lng, radiusMeters));
            }
        }

        return clauses;
    }
}
