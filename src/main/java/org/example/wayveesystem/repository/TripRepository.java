package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.Trip;
import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    List<Trip> findByUser(User user);

    long countByCreatedAtAfter(LocalDateTime from);

    // Group trips by day
    @Query("SELECT CAST(t.createdAt AS date) as d, COUNT(t) " +
            "FROM Trip t WHERE t.createdAt >= :from " +
            "GROUP BY CAST(t.createdAt AS date) ORDER BY d")
    List<Object[]> tripsGroupedByDay(@Param("from") LocalDateTime from);

    @Query("SELECT FUNCTION('date_part', 'year', t.createdAt), FUNCTION('date_part', 'week', t.createdAt), COUNT(t) " +
            "FROM Trip t WHERE t.createdAt >= :from " +
            "GROUP BY FUNCTION('date_part', 'year', t.createdAt), FUNCTION('date_part', 'week', t.createdAt) " +
            "ORDER BY FUNCTION('date_part', 'year', t.createdAt), FUNCTION('date_part', 'week', t.createdAt)")
    List<Object[]> tripsGroupedByWeek(@Param("from") LocalDateTime from);

    @Query("SELECT FUNCTION('date_part', 'year', t.createdAt), FUNCTION('date_part', 'month', t.createdAt), COUNT(t) " +
            "FROM Trip t WHERE t.createdAt >= :from " +
            "GROUP BY FUNCTION('date_part', 'year', t.createdAt), FUNCTION('date_part', 'month', t.createdAt) " +
            "ORDER BY FUNCTION('date_part', 'year', t.createdAt), FUNCTION('date_part', 'month', t.createdAt)")
    List<Object[]> tripsGroupedByMonth(@Param("from") LocalDateTime from);
}
