package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.LocationOpeningHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationOpeningHourRepository extends JpaRepository<LocationOpeningHour, Long> {
    List<LocationOpeningHour> findByOsmId(Long osmId);
    Optional<LocationOpeningHour> findByOsmIdAndDayOfWeek(Long osmId, DayOfWeek dayOfWeek);
}
