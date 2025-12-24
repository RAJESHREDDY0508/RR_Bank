package com.RRBank.banking.repository;

import com.RRBank.banking.entity.FraudEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Fraud Event Repository
 */
@Repository
public interface FraudEventRepository extends JpaRepository<FraudEvent, UUID> {

    /**
     * Find fraud event by transaction ID
     */
    Optional<FraudEvent> findByTransactionId(UUID transactionId);

    /**
     * Find all fraud events for an account
     */
    List<FraudEvent> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    /**
     * Find all fraud events for a customer
     */
    List<FraudEvent> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /**
     * Find by risk level
     */
    List<FraudEvent> findByRiskLevelOrderByCreatedAtDesc(FraudEvent.RiskLevel riskLevel);

    /**
     * Find by status
     */
    List<FraudEvent> findByStatusOrderByCreatedAtDesc(FraudEvent.FraudStatus status);

    /**
     * Find high risk events pending review
     */
    @Query("SELECT f FROM FraudEvent f WHERE f.status = 'PENDING_REVIEW' " +
           "AND (f.riskLevel = 'HIGH' OR f.riskLevel = 'CRITICAL') " +
           "ORDER BY f.riskScore DESC, f.createdAt DESC")
    List<FraudEvent> findHighRiskPendingReview();

    /**
     * Find recent fraud events (last 24 hours)
     */
    @Query("SELECT f FROM FraudEvent f WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<FraudEvent> findRecentEvents(@Param("since") LocalDateTime since);

    /**
     * Count fraud events by account in time window
     */
    @Query("SELECT COUNT(f) FROM FraudEvent f WHERE f.accountId = :accountId " +
           "AND f.createdAt >= :since")
    long countByAccountIdSince(@Param("accountId") UUID accountId, 
                               @Param("since") LocalDateTime since);

    /**
     * Count confirmed fraud for account
     */
    long countByAccountIdAndStatus(UUID accountId, FraudEvent.FraudStatus status);

    /**
     * Find events by date range
     */
    @Query("SELECT f FROM FraudEvent f WHERE f.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY f.createdAt DESC")
    List<FraudEvent> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);
}
