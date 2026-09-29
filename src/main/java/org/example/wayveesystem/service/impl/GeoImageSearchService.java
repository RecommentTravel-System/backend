package org.example.wayveesystem.service.impl;

import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.service.ImageCacheStore;
import org.example.wayveesystem.service.ImageResolution;
import org.example.wayveesystem.service.MapillaryService;
import org.example.wayveesystem.service.WikimediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GeoImageSearchService {
    private final MapillaryService mapillaryService;
    private final WikimediaService wikimediaService;
    private final ImageCacheStore imageCacheStore;

    public GeoImageSearchService(MapillaryService mapillaryService, WikimediaService wikimediaService,
                                 ImageCacheStore imageCacheStore) {
        this.mapillaryService = mapillaryService;
        this.wikimediaService = wikimediaService;
        this.imageCacheStore = imageCacheStore;
    }

    /**
     * Resolves a fallback image in the requested priority order.  This must be
     * synchronous because callers need the resolved URL in the current API
     * response, rather than only in a later request after an async task ends.
     */
    public ImageResolution searchAndCache(OsmPlace place) {
        if (place == null || place.osmId() == null || place.latitude() == null || place.longitude() == null) {
            return new ImageResolution(null, null);
        }

        try {
            String url = wikimediaService.searchByNameAndLocation(place.name(), place.latitude(), place.longitude());
            if (url != null && !url.isBlank()) {
                ImageResolution resolution = new ImageResolution(url, ImageSource.WIKIMEDIA);
                imageCacheStore.put(place.osmId(), resolution);
                return resolution;
            }
        } catch (RuntimeException ex) {
            log.warn("Wikimedia image lookup failed for OSM {} ({}): {}",
                    place.osmId(), place.name(), ex.getMessage());
        }

        try {
            String url = mapillaryService.searchNearby(place.latitude(), place.longitude());
            if (url != null && !url.isBlank()) {
                ImageResolution resolution = new ImageResolution(url, ImageSource.MAPILLARY);
                imageCacheStore.put(place.osmId(), resolution);
                return resolution;
            }
        } catch (RuntimeException ex) {
            log.warn("Mapillary image lookup failed for OSM {} ({}): {}",
                    place.osmId(), place.name(), ex.getMessage());
        }

        return new ImageResolution(null, null);
    }
}
