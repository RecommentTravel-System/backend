package org.example.wayveesystem.service.poi.strategy;

import org.example.wayveesystem.common.enums.LocationSource;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PoiSourceStrategyFactory {

    private final Map<LocationSource, PoiSourceStrategy> strategyMap = new EnumMap<>(LocationSource.class);

    public PoiSourceStrategyFactory(List<PoiSourceStrategy> strategies) {
        for (PoiSourceStrategy strategy : strategies) {
            strategyMap.put(strategy.getSource(), strategy);
        }
    }

    public PoiSourceStrategy getStrategy(LocationSource source) {
        PoiSourceStrategy strategy = strategyMap.get(source);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported POI source: " + source);
        }
        return strategy;
    }
}
