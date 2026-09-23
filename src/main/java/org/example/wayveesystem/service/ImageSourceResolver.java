package org.example.wayveesystem.service;

import org.example.wayveesystem.common.enums.ImageSource;
import org.example.wayveesystem.dto.OsmPlace;

import java.util.Optional;

public interface ImageSourceResolver {
    Optional<String> tryResolve(OsmPlace place);
    ImageSource sourceType();
}
