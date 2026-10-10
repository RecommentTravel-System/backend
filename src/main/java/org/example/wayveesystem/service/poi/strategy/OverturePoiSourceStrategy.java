package org.example.wayveesystem.service.poi.strategy;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OverturePoiSourceStrategy implements PoiSourceStrategy {

    private static final Set<String> CLOSED_STATUSES = Set.of(
            "closed", "permanently_closed", "temporarily_closed", "inactive"
    );

    @Override
    public LocationSource getSource() {
        return LocationSource.OVERTURE;
    }

    @Override
    public boolean shouldSkipByStatus(String operatingStatus) {
        if (operatingStatus == null || operatingStatus.isBlank()) {
            return false;
        }
        return CLOSED_STATUSES.contains(operatingStatus.trim().toLowerCase());
    }

    @Override
    public boolean shouldSkipByConfidence(Float confidence, float minConfidence) {
        // Skip Overture records whose confidence is below the configured threshold
        if (confidence == null) {
            return false;
        }
        return confidence < minConfidence;
    }

    @Override
    public PoiStatus resolveStatus(String operatingStatus) {
        if (operatingStatus == null || operatingStatus.isBlank()) {
            return PoiStatus.ACTIVE;
        }
        String clean = operatingStatus.trim().toLowerCase();
        if (CLOSED_STATUSES.contains(clean)) {
            return PoiStatus.CLOSED;
        }
        return PoiStatus.ACTIVE;
    }
}
