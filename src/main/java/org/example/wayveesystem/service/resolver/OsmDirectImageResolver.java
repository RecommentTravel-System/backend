package org.example.wayveesystem.service.resolver;

import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.dto.OsmPlace;
import org.example.wayveesystem.service.ImageSourceResolver;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(1)
public class OsmDirectImageResolver implements ImageSourceResolver {
    @Override
    public Optional<String> tryResolve(OsmPlace place) {
        return Optional.ofNullable(place.imageUrl());
    }

    @Override
    public ImageSource sourceType() {
        return ImageSource.OSM;
    }
}