package org.example.wayveesystem.common.scheduler;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.dto.GridCacheKey;
import org.example.wayveesystem.dto.OsmPlace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoiCachePrewarmer {

    private final AsyncLoadingCache<GridCacheKey, List<OsmPlace>> poiAsyncLoadingCache;

    @Value("${wayvee.cache.grid-precision:0.02}")
    private double gridPrecision;

    // Toạ độ trung tâm các khu vực đông user nhất (vd: Q1, Q3, Bình Thạnh...)
    private static final double[][] HOTSPOTS = {
            {10.7769, 106.7009}, // Q1
            {10.7867, 106.6957}, // Phú Nhuận
            {10.8010, 106.7107}, // Bình Thạnh
    };

    @Scheduled(cron = "0 0 3 * * *") // 3h sáng mỗi ngày, ít traffic
    public void prewarmHotspots() {
        log.info("Pre-warming POI cache for {} hotspots", HOTSPOTS.length);
        for (double[] point : HOTSPOTS) {
            double gridLat = GridCacheKey.snapToGrid(point[0], gridPrecision);
            double gridLng = GridCacheKey.snapToGrid(point[1], gridPrecision);
            GridCacheKey key = new GridCacheKey(gridLat, gridLng, 3000);
            poiAsyncLoadingCache.get(key); // trigger async load, không cần đợi kết quả ở đây
        }
    }
}
