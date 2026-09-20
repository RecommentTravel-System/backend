package org.example.wayveesystem.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${wayvee.cache.poi-ttl-minutes}")
    private int poiTtlMinutes;

    @Value("${wayvee.cache.poi-max-size}")
    private int poiMaxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("poiCache", "reverseGeocodeCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(poiMaxSize)
                .expireAfterWrite(poiTtlMinutes, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}
