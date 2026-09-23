package org.example.wayveesystem.service;

import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.service.impl.GeoImageSearchService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ImageResolverService {

    private final List<ImageSourceResolver> resolvers;
    private final ImageCacheStore imageCacheStore;
    private final GeoImageSearchService geoImageSearchService;

    public ImageResolverService(List<ImageSourceResolver> resolvers, ImageCacheStore imageCacheStore,
                                GeoImageSearchService geoImageSearchService) {
        this.resolvers = List.copyOf(Objects.requireNonNull(resolvers, "resolvers must not be null"));
        this.imageCacheStore = Objects.requireNonNull(imageCacheStore, "imageCacheStore must not be null");
        this.geoImageSearchService = Objects.requireNonNull(geoImageSearchService, "geoImageSearchService must not be null");
    }

    public ImageResolution resolve(OsmPlace place) {
        if (place == null) {
            return new ImageResolution(null, null);
        }

        ImageResolution cached = imageCacheStore.get(place.osmId());
        if (cached != null) {
            return cached;
        }

        for (ImageSourceResolver resolver : resolvers) {
            Optional<String> url = resolver.tryResolve(place);
            if (url.isPresent()) {
                ImageResolution result = new ImageResolution(url.get(), resolver.sourceType());
                imageCacheStore.put(place.osmId(), result);
                return result;
            }
        }

        return geoImageSearchService.searchAndCache(place);
    }
}
