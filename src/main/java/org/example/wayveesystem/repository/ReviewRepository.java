package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.Review;
import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByOsmId(Long osmId);
    List<Review> findByUser(User user);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.osmId = :osmId")
    Double getAverageRatingByOsmId(@Param("osmId") Long osmId);

    long countByOsmId(Long osmId);

    List<Review> findByOsmIdAndRatingGreaterThanEqual(Long osmId, Integer minRating);
    List<Review> findByUserAndPlaceNameContainingIgnoreCase(User user, String placeName);
}
