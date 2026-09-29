package org.example.wayveesystem.service.impl;

import org.example.wayveesystem.service.ImageCacheStore;
import org.example.wayveesystem.service.ImageResolution;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryImageCacheStore implements ImageCacheStore {
    private final Map<Long, ImageResolution> cache = new ConcurrentHashMap<>();

    @Override
    public ImageResolution get(Long osmId) {
        return osmId == null ? null : cache.get(osmId);
    }

    @Override
    public void put(Long osmId, ImageResolution resolution) {
        if (osmId != null) cache.put(osmId, resolution);
    }
}