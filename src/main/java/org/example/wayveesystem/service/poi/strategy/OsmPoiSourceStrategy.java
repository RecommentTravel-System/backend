package org.example.wayveesystem.service.poi.strategy;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OsmPoiSourceStrategy implements PoiSourceStrategy {

    private static final Set<String> CLOSED_STATUSES = Set.of(
            "closed", "permanently_closed", "temporarily_closed", "abandoned", "disused", "demolished"
    );

    @Override
    public LocationSource getSource() {
        return LocationSource.OSM;
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
        // OSM does not supply confidence values; confidence is not filtered
        return false;
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
