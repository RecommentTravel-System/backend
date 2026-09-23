package org.example.wayveesystem.service.resolver;

import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.service.ImageSourceResolver;
import org.example.wayveesystem.service.WikimediaService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(2)
public class WikimediaTagResolver implements ImageSourceResolver {
    private final WikimediaService wikimediaService;

    public WikimediaTagResolver(WikimediaService wikimediaService) {
        this.wikimediaService = wikimediaService;
    }

    @Override
    public Optional<String> tryResolve(OsmPlace place) {
        String tag = place.tags() == null ? null : place.tags().get("wikimedia_commons");
        if (tag == null || tag.isBlank()) return Optional.empty();
        try {
            String url = wikimediaService.resolveImage(tag);
            return (url != null && !url.isBlank()) ? Optional.of(url) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public ImageSource sourceType() {
        return ImageSource.WIKIMEDIA;
    }
}
