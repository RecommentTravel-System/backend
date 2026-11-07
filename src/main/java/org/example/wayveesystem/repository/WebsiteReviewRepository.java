package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.WebsiteReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebsiteReviewRepository extends JpaRepository<WebsiteReview, Long> {

    List<WebsiteReview> findAllByOrderByCreatedAtDesc();

    List<WebsiteReview> findByRatingOrderByCreatedAtDesc(Integer rating);

    @Query("SELECT AVG(r.rating) FROM WebsiteReview r")
    Double getAverageRating();

    long countByRating(Integer rating);

    List<WebsiteReview> findByUserUserIdOrderByCreatedAtDesc(Long userId);
}

