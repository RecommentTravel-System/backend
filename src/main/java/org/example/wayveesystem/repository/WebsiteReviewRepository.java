package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.WebsiteReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT r FROM WebsiteReview r WHERE (r.user.userId = :userId OR (r.userEmail IS NOT NULL AND LOWER(r.userEmail) = LOWER(:email))) ORDER BY r.createdAt DESC")
    List<WebsiteReview> findByUserOrEmail(@Param("userId") Long userId, @Param("email") String email);
}

