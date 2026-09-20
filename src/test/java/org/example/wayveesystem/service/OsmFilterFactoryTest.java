package org.example.wayveesystem.service;

import org.example.wayveesystem.common.strategy.CategoryOsmFilterStrategy;
import org.example.wayveesystem.common.strategy.CuisineOsmFilterStrategy;
import org.example.wayveesystem.common.strategy.OsmFilterFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class OsmFilterFactoryTest {

    private OsmFilterFactory osmFilterFactory;

    @BeforeEach
    void setUp() {
        osmFilterFactory = new OsmFilterFactory(List.of(
                new CategoryOsmFilterStrategy(),
                new CuisineOsmFilterStrategy()
        ));
    }

    @Test
    void buildFilterClauses_RestaurantAndCafe_ShouldBuildOverpassClauses() {
        double lat = 10.7769;
        double lng = 106.7009;
        int radius = 1000;

        List<String> clauses = osmFilterFactory.buildFilterClauses(
                "category",
                List.of("RESTAURANT", "CAFE"),
                lat,
                lng,
                radius
        );

        Assertions.assertNotNull(clauses);
        Assertions.assertFalse(clauses.isEmpty());

        boolean containsRestaurant = clauses.stream().anyMatch(c -> c.contains("restaurant"));
        boolean containsCafe = clauses.stream().anyMatch(c -> c.contains("cafe"));

        Assertions.assertTrue(containsRestaurant, "Clauses should contain restaurant");
        Assertions.assertTrue(containsCafe, "Clauses should contain cafe");
    }

    @Test
    void buildFilterClauses_AllEightCategories_ShouldProduceGroupedRegexClauses() {
        double lat = 10.7769;
        double lng = 106.7009;
        int radius = 1000;

        List<String> categories = List.of(
                "RESTAURANT", "CAFE", "FAST_FOOD", "BAR", "BAKERY", "SHOPPING", "ATTRACTION", "ACCOMMODATION"
        );

        List<String> clauses = osmFilterFactory.buildFilterClauses("category", categories, lat, lng, radius);

        Assertions.assertNotNull(clauses);
        Assertions.assertFalse(clauses.isEmpty());
        // 5 tag keys (amenity, shop, tourism, leisure, craft) * 2 (node + way) = 10 clauses max
        Assertions.assertTrue(clauses.size() <= 10, "Clauses count should be minimized by regex grouping (<= 10 clauses)");

        // Verify amenity regex group
        boolean hasAmenityRegex = clauses.stream().anyMatch(c -> c.contains("node[\"amenity\"~\"^("));
        Assertions.assertTrue(hasAmenityRegex, "Should contain grouped regex for amenity tag key");
    }
}

