package org.example.wayveesystem.configuration;


import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.service.impl.PoiGridFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class CacheConfig {

    @Value("${wayvee.cache.poi.max-size:5000}")
    private long maxSize;

    @Value("${wayvee.cache.poi.expire-hours:12}")
    private long expireHours;

    @Value("${wayvee.cache.poi.refresh-hours:6}")
    private long refreshHours;

    /**
     * Dedicated executor for Overpass batch fetching (parallel category batches)
     * and for Caffeine's async refresh, so it never competes with Tomcat's request threads.
     */
    @Bean(name = "overpassExecutor")
    public ExecutorService overpassExecutor() {
        return Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "overpass-fetch-");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * AsyncLoadingCache: when multiple threads request the same key while it's missing,
     * Caffeine guarantees only ONE load() call runs; all callers share the same Future.
     * This is what solves the "many users at once" stampede problem.
     */
    @Bean
    public AsyncLoadingCache<GridCacheKey, List<OsmPlace>> poiAsyncLoadingCache(
            PoiGridFetcher poiGridFetcher,
            ExecutorService overpassExecutor
    ) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(Duration.ofHours(expireHours))
                .refreshAfterWrite(Duration.ofHours(refreshHours))
                .recordStats()
                .executor(overpassExecutor)
                .buildAsync((key, executor) ->
                        java.util.concurrent.CompletableFuture.supplyAsync(
                                () -> poiGridFetcher.fetchAllCategoriesForGrid(key), executor));
    }
}
