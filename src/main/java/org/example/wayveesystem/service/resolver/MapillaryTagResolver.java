package org.example.wayveesystem.service.resolver;

import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.service.ImageSourceResolver;
import org.example.wayveesystem.service.MapillaryService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Optional;

@Component
@Order(3)
public class MapillaryTagResolver implements ImageSourceResolver {
    private final MapillaryService mapillaryService;

    public MapillaryTagResolver(MapillaryService mapillaryService) {
        this.mapillaryService = mapillaryService;
    }

    @Override
    public Optional<String> tryResolve(OsmPlace place) {
        String tag = place.tags() == null ? null : place.tags().get("mapillary");
        if (tag == null || tag.isBlank()) return Optional.empty();
        try {
            String url = mapillaryService.resolveImage(tag);
            return isValid(url) ? Optional.of(url) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public ImageSource sourceType() {
        return ImageSource.MAPILLARY;
    }

    private boolean isValid(String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        try {
            URI uri = URI.create(candidate);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
