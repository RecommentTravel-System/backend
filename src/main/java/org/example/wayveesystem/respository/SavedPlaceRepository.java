package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.SavedPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    Optional<SavedPlace> findByOsmId(Long osmId);

    boolean existsByOsmId(Long osmId);
}
