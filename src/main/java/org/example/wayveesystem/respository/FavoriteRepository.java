package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.Favorite;
import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndOsmId(User user, Long osmId);
    boolean existsByUserAndOsmId(User user, Long osmId);
}
