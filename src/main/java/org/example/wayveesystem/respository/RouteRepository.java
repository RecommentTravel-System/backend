package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.Route;
import org.example.wayveesystem.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByTrip(Trip trip);
}
