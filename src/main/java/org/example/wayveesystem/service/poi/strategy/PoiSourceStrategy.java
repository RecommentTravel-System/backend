package org.example.wayveesystem.service.poi.strategy;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;

public interface PoiSourceStrategy {

    LocationSource getSource();

    /**
     * Determines whether the row should be skipped based on operating status string.
     */
    boolean shouldSkipByStatus(String operatingStatus);

    /**
     * Determines whether the row should be skipped based on confidence score.
     */
    boolean shouldSkipByConfidence(Float confidence, float minConfidence);

    /**
     * Resolves the entity PoiStatus from the raw operating status.
     */
    PoiStatus resolveStatus(String operatingStatus);
}
