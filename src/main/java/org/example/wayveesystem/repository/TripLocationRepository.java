package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.Trip;
import org.example.wayveesystem.model.TripLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripLocationRepository extends JpaRepository<TripLocation, Long> {
    List<TripLocation> findByTripOrderByVisitOrderAsc(Trip trip);
}
