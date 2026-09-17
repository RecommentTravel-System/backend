package org.example.wayveesystem.respository;

import org.example.wayveesystem.model.Location;
import org.example.wayveesystem.model.Review;
import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByLocation(Location location);
    List<Review> findByUser(User user);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.location = :location")
    Double getAverageRatingByLocation(@Param("location") Location location);

    long countByLocation(Location location);
}
