package org.example.wayveesystem.common.strategy;

import java.util.List;

public interface OsmFilterStrategy {

    /**
     * Checks whether this strategy supports the given filter key (e.g. "category", "cuisine", "shop").
     */
    boolean supports(String filterKey);

    /**
     * Builds Overpass QL query clauses for nodes/ways/relations within radius.
     */
    List<String> buildQueryClauses(String filterValue, double lat, double lng, int radiusMeters);
}
