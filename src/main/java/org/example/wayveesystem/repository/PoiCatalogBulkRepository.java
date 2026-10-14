package org.example.wayveesystem.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.model.PoiCatalog;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

/**
 * High-performance bulk upsert repository using PostgreSQL native ON CONFLICT clause.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PoiCatalogBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String UPSERT_SQL = """
            INSERT INTO poi_catalog (
                source, source_id, name, category_id, address, lat, lon, confidence, status, imported_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (source, source_id) DO UPDATE SET
                name = EXCLUDED.name,
                category_id = EXCLUDED.category_id,
                address = EXCLUDED.address,
                lat = EXCLUDED.lat,
                lon = EXCLUDED.lon,
                confidence = EXCLUDED.confidence,
                status = EXCLUDED.status,
                imported_at = EXCLUDED.imported_at
            """;

    /**
     * Executes batch upsert for a chunk of POI records in a dedicated transaction.
     *
     * @param items List of POI catalog items to upsert.
     * @return Number of records affected.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int batchUpsert(List<PoiCatalog> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        int[] updateCounts = jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PoiCatalog poi = items.get(i);
                ps.setString(1, poi.getSource().name());
                ps.setString(2, poi.getSourceId());
                ps.setString(3, poi.getName());

                if (poi.getCategoryId() != null) {
                    ps.setLong(4, poi.getCategoryId());
                } else {
                    ps.setNull(4, Types.BIGINT);
                }

                if (poi.getAddress() != null) {
                    ps.setString(5, poi.getAddress());
                } else {
                    ps.setNull(5, Types.VARCHAR);
                }

                ps.setFloat(6, poi.getLat());
                ps.setFloat(7, poi.getLon());

                if (poi.getConfidence() != null) {
                    ps.setFloat(8, poi.getConfidence());
                } else {
                    ps.setNull(8, Types.REAL);
                }

                ps.setString(9, poi.getStatus().name());
                ps.setTimestamp(10, Timestamp.valueOf(poi.getImportedAt()));
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });

        return updateCounts.length;
    }
}
