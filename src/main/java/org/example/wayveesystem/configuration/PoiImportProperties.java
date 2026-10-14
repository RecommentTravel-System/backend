package org.example.wayveesystem.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "wayvee.poi-import")
public class PoiImportProperties {

    /**
     * Batch size for bulk upsert operations into PostgreSQL.
     */
    private int batchSize = 1000;

    /**
     * Minimum confidence threshold for Overture Map records (0.0 to 1.0).
     */
    private float minConfidence = 0.6f;

    /**
     * Default server-side file path if none specified in request.
     */
    private String defaultServerFilePath = "data/poi_vietnam.csv";

    /**
     * Vietnam geographical bounding box boundary limits.
     */
    private BoundingBox boundingBox = new BoundingBox(8.18f, 23.39f, 102.14f, 109.46f);

    /**
     * Mapping from raw OSM/Overture category strings (lowercased) to System Category Code.
     */
    private Map<String, String> categoryMapping = new HashMap<>();

    @Getter
    @Setter
    public static class BoundingBox {
        private float minLat = 8.18f;
        private float maxLat = 23.39f;
        private float minLon = 102.14f;
        private float maxLon = 109.46f;

        public BoundingBox() {
        }

        public BoundingBox(float minLat, float maxLat, float minLon, float maxLon) {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.minLon = minLon;
            this.maxLon = maxLon;
        }

        public boolean contains(float lat, float lon) {
            return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
        }
    }
}
