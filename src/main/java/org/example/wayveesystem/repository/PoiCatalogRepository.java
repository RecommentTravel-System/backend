package org.example.wayveesystem.repository;

import org.example.wayveesystem.common.enums.LocationSource;
import org.example.wayveesystem.common.enums.PoiStatus;
import org.example.wayveesystem.model.PoiCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoiCatalogRepository extends JpaRepository<PoiCatalog, Long> {

    Optional<PoiCatalog> findBySourceAndSourceId(LocationSource source, String sourceId);

    long countBySource(LocationSource source);

    List<PoiCatalog> findByLatBetweenAndLonBetweenAndStatus(
            Float minLat, Float maxLat, Float minLon, Float maxLon, PoiStatus status
    );
}
