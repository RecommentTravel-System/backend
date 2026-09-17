package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findBySourceOsmId(Long sourceOsmId);

    List<Location> findBySourceOsmIdIn(List<Long> sourceOsmIds);

    @Query(value = """
            SELECT l.* FROM locations l
            WHERE (6371000 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) *
                   cos(radians(l.longitude) - radians(:lng)) +
                   sin(radians(:lat)) * sin(radians(l.latitude)))) <= :radius
            """, nativeQuery = true)
    List<Location> findNearbyLocations(@Param("lat") double lat,
                                       @Param("lng") double lng,
                                       @Param("radius") double radiusMeters);
}
