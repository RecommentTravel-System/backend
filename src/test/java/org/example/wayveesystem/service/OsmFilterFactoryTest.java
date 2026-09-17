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
    void buildFilterClauses_Cuisine_ShouldBuildCuisineClauses() {
        double lat = 10.7769;
        double lng = 106.7009;
        int radius = 1000;

        List<String> clauses = osmFilterFactory.buildFilterClauses(
                "cuisine",
                List.of("vietnamese"),
                lat,
                lng,
                radius
        );

        Assertions.assertNotNull(clauses);
        Assertions.assertFalse(clauses.isEmpty());
        Assertions.assertTrue(clauses.get(0).contains("cuisine\"=\"vietnamese"));
    }
}
