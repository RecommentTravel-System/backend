package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.Payment;
import org.example.wayveesystem.model.Subscription;
import org.example.wayveesystem.common.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findBySubscription(Subscription subscription);
    Optional<Payment> findByTransactionCode(String transactionCode);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.paidAt >= :from")
    BigDecimal sumAmountByStatusFrom(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from);

    @Query("SELECT CAST(p.paidAt AS date) as d, COALESCE(SUM(p.amount), 0) " +
            "FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt >= :from " +
            "GROUP BY CAST(p.paidAt AS date) ORDER BY d")
    List<Object[]> revenueGroupedByDay(@Param("from") LocalDateTime from);

    @Query("SELECT FUNCTION('date_part', 'year', p.paidAt), FUNCTION('date_part', 'week', p.paidAt), COALESCE(SUM(p.amount), 0) " +
            "FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt >= :from " +
            "GROUP BY FUNCTION('date_part', 'year', p.paidAt), FUNCTION('date_part', 'week', p.paidAt) " +
            "ORDER BY FUNCTION('date_part', 'year', p.paidAt), FUNCTION('date_part', 'week', p.paidAt)")
    List<Object[]> revenueGroupedByWeek(@Param("from") LocalDateTime from);

    @Query("SELECT FUNCTION('date_part', 'year', p.paidAt), FUNCTION('date_part', 'month', p.paidAt), COALESCE(SUM(p.amount), 0) " +
            "FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt >= :from " +
            "GROUP BY FUNCTION('date_part', 'year', p.paidAt), FUNCTION('date_part', 'month', p.paidAt) " +
            "ORDER BY FUNCTION('date_part', 'year', p.paidAt), FUNCTION('date_part', 'month', p.paidAt)")
    List<Object[]> revenueGroupedByMonth(@Param("from") LocalDateTime from);
}
