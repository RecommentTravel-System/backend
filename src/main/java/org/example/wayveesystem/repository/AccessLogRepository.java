package org.example.wayveesystem.repository;

import org.example.wayveesystem.model.AccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {

    long countByCreatedAtAfter(LocalDateTime time);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AccessLog a WHERE a.createdAt >= :time")
    long countDistinctIpAddressByCreatedAtAfter(@Param("time") LocalDateTime time);

    @Query("SELECT COUNT(DISTINCT a.ipAddress) FROM AccessLog a")
    long countDistinctIpAddress();

    @Query("SELECT a.path, a.pageTitle, COUNT(a.id) as cnt FROM AccessLog a GROUP BY a.path, a.pageTitle ORDER BY cnt DESC")
    List<Object[]> findTopVisitedPages(Pageable pageable);

    @Query("SELECT a.deviceType, COUNT(a.id) FROM AccessLog a GROUP BY a.deviceType")
    List<Object[]> findDeviceTypeCounts();

    @Query("SELECT FUNCTION('DATE', a.createdAt) as logDate, COUNT(a.id) as totalVisits, COUNT(DISTINCT a.ipAddress) as uniqueIps " +
           "FROM AccessLog a " +
           "WHERE a.createdAt >= :since " +
           "GROUP BY FUNCTION('DATE', a.createdAt) " +
           "ORDER BY logDate ASC")
    List<Object[]> findDailyTrafficSince(@Param("since") LocalDateTime since);

    void deleteByCreatedAtBefore(LocalDateTime time);
}
