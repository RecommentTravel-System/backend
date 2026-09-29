package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByCreatedAtAfter(LocalDateTime from);

    @Query("SELECT CAST(u.createdAt AS date) as d, COUNT(u) " +
            "FROM User u WHERE u.createdAt >= :from " +
            "GROUP BY CAST(u.createdAt AS date) ORDER BY d")
    List<Object[]> usersGroupedByDay(@Param("from") LocalDateTime from);

    @Query("SELECT FUNCTION('date_part', 'year', u.createdAt), FUNCTION('date_part', 'week', u.createdAt), COUNT(u) " +
            "FROM User u WHERE u.createdAt >= :from " +
            "GROUP BY FUNCTION('date_part', 'year', u.createdAt), FUNCTION('date_part', 'week', u.createdAt) " +
            "ORDER BY FUNCTION('date_part', 'year', u.createdAt), FUNCTION('date_part', 'week', u.createdAt)")
    List<Object[]> usersGroupedByWeek(@Param("from") LocalDateTime from);

    @Query("SELECT FUNCTION('date_part', 'year', u.createdAt), FUNCTION('date_part', 'month', u.createdAt), COUNT(u) " +
            "FROM User u WHERE u.createdAt >= :from " +
            "GROUP BY FUNCTION('date_part', 'year', u.createdAt), FUNCTION('date_part', 'month', u.createdAt) " +
            "ORDER BY FUNCTION('date_part', 'year', u.createdAt), FUNCTION('date_part', 'month', u.createdAt)")
    List<Object[]> usersGroupedByMonth(@Param("from") LocalDateTime from);

}
