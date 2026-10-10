package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.Trip;
import org.example.wayveesystem.model.TripLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {
    List<TripLocation> findByTripOrderByVisitOrderAsc(Trip trip);
    List<TripLocation> findByTripOrderByPlannedDayAscVisitOrderAsc(Trip trip);
    List<TripLocation> findByTripAndPlannedDayOrderByVisitOrderAsc(Trip trip, Integer plannedDay);
    Optional<TripLocation> findByTripLocationIdAndTrip(Long tripLocationId, Trip trip);
}
