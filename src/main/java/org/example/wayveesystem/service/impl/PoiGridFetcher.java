package org.example.wayveesystem.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.client.OverpassApiClient;
import org.example.wayveesystem.common.strategy.OsmFilterFactory;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.dto.request.OverpassRequest;
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.example.wayveesystem.mapper.OsmPlaceMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoiGridFetcher {

    private static final int MAX_GRID_RADIUS_METERS = 3000;
    private static final List<List<String>> CATEGORY_BATCHES = List.of(
            List.of("RESTAURANT", "CAFE", "FAST_FOOD"),
            List.of("BAR", "BAKERY", "SHOPPING"),
            List.of("ATTRACTION", "ACCOMMODATION")
    );

    private final OverpassApiClient overpassApiClient;
    private final OsmFilterFactory osmFilterFactory;
    private final OsmPlaceMapper osmPlaceMapper;

    @Qualifier("overpassExecutor")
    private final ExecutorService overpassExecutor;

    /**
     * Fetches all category batches for a grid cell IN PARALLEL.
     * Runs on overpassExecutor, called at most once per key at a time
     * thanks to Caffeine's AsyncLoadingCache (see CacheConfig).
     */
    public List<OsmPlace> fetchAllCategoriesForGrid(GridCacheKey cacheKey) {
        log.info("Cache MISS/refresh for grid [{}, {}] — fetching {} category batches in parallel",
                cacheKey.gridLat(), cacheKey.gridLng(), CATEGORY_BATCHES.size());

        List<CompletableFuture<List<OsmPlace>>> futures = CATEGORY_BATCHES.stream()
                .map(batch -> CompletableFuture.supplyAsync(
                        () -> fetchBatch(batch, cacheKey), overpassExecutor))
                .toList();

        // Wait for all batches; individual failures are swallowed inside fetchBatch
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Set<Long> seenOsmIds = new HashSet<>();
        List<OsmPlace> combined = new ArrayList<>();
        for (CompletableFuture<List<OsmPlace>> f : futures) {
            for (OsmPlace place : f.join()) {
                if (place != null && place.osmId() != null && seenOsmIds.add(place.osmId())) {
                    combined.add(place);
                }
            }
        }

        log.info("Grid [{}, {}] loaded {} unique POIs", cacheKey.gridLat(), cacheKey.gridLng(), combined.size());
        return combined;
    }

    private List<OsmPlace> fetchBatch(List<String> batch, GridCacheKey cacheKey) {
        try {
            List<String> queryClauses = osmFilterFactory.buildFilterClauses(
                    "category", batch, cacheKey.gridLat(), cacheKey.gridLng(), MAX_GRID_RADIUS_METERS
            );
            OverpassRequest overpassRequest = OverpassRequest.of(
                    cacheKey.gridLat(), cacheKey.gridLng(), MAX_GRID_RADIUS_METERS, queryClauses
            );
            OverpassResponse response = overpassApiClient.fetchNearbyPois(overpassRequest);

            List<OsmPlace> result = new ArrayList<>();
            if (response != null && response.elements() != null) {
                for (var elem : response.elements()) {
                    OsmPlace rawPlace = osmPlaceMapper.toOsmPlace(elem);
                    if (rawPlace != null) {
                        result.add(rawPlace);
                    }
                }
            }
            return result;
        } catch (Exception ex) {
            log.warn("Error fetching POI batch {} for grid [{}, {}]: {}",
                    batch, cacheKey.gridLat(), cacheKey.gridLng(), ex.getMessage());
            return List.of(); // fail this batch only, others still return
        }
    }
}