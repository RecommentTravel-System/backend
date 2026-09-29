package org.example.wayveesystem.common.strategy;

import java.util.ArrayList;
import java.util.List;

public interface OsmFilterStrategy {

    /**
     * Checks whether this strategy supports the given filter key (e.g. "category", "cuisine", "shop").
     */
    boolean supports(String filterKey);

    /**
     * Builds Overpass QL query clauses for nodes/ways/relations within radius for a single value.
     */
    List<String> buildQueryClauses(String filterValue, double lat, double lng, int radiusMeters);

    /**
     * Builds grouped Overpass QL query clauses for multiple values by regex grouping.
     */
    default List<String> buildGroupedQueryClauses(List<String> filterValues, double lat, double lng, int radiusMeters) {
        List<String> clauses = new ArrayList<>();
        if (filterValues != null) {
            for (String val : filterValues) {
                clauses.addAll(buildQueryClauses(val, lat, lng, radiusMeters));
            }
        }
        return clauses;
    }
}

