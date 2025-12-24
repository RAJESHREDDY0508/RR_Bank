package com.RRBank.banking.repository;

import com.RRBank.banking.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Audit Log Repository
 * READ-ONLY repository for immutable audit trail
 * No delete or update methods to maintain integrity
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find all audit logs for a customer
     */
    Page<AuditLog> findByCustomerIdOrderByTimestampDesc(UUID customerId, Pageable pageable);

    /**
     * Find all audit logs for an account
     */
    Page<AuditLog> findByAccountIdOrderByTimestampDesc(UUID accountId, Pageable pageable);

    /**
     * Find all audit logs for a user
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    /**
     * Find all audit logs by event type
     */
    Page<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);

    /**
     * Find all audit logs by severity
     */
    Page<AuditLog> findBySeverityOrderByTimestampDesc(AuditLog.Severity severity, Pageable pageable);

    /**
     * Find all compliance flagged audit logs
     */
    Page<AuditLog> findByComplianceFlagTrueOrderByTimestampDesc(Pageable pageable);

    /**
     * Find all sensitive audit logs
     */
    Page<AuditLog> findByIsSensitiveTrueOrderByTimestampDesc(Pageable pageable);

    /**
     * Find audit logs within date range
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate, 
            LocalDateTime endDate, 
            Pageable pageable
    );

    /**
     * Find audit logs by entity type and entity ID
     */
    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType, 
            UUID entityId, 
            Pageable pageable
    );

    /**
     * Count audit logs by event type
     */
    long countByEventType(String eventType);

    /**
     * Count audit logs by severity
     */
    long countBySeverity(AuditLog.Severity severity);

    /**
     * Count compliance flagged logs
     */
    long countByComplianceFlagTrue();

    /**
     * Find recent high severity events
     */
    @Query("SELECT a FROM AuditLog a WHERE a.severity IN ('ERROR', 'CRITICAL') " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findRecentHighSeverityEvents(Pageable pageable);

    /**
     * Complex search query with multiple filters
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:eventSource IS NULL OR a.eventSource = :eventSource) AND " +
           "(:severities IS NULL OR a.severity IN :severities) AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:entityId IS NULL OR a.entityId = :entityId) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:customerId IS NULL OR a.customerId = :customerId) AND " +
           "(:accountId IS NULL OR a.accountId = :accountId) AND " +
           "(:action IS NULL OR a.action LIKE %:action%) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) AND " +
           "(:complianceFlag IS NULL OR a.complianceFlag = :complianceFlag) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("eventType") String eventType,
            @Param("eventSource") String eventSource,
            @Param("severities") List<AuditLog.Severity> severities,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("userId") UUID userId,
            @Param("customerId") UUID customerId,
            @Param("accountId") UUID accountId,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("complianceFlag") Boolean complianceFlag,
            Pageable pageable
    );

    /**
     * Get audit statistics for a customer
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a " +
           "WHERE a.customerId = :customerId " +
           "GROUP BY a.eventType")
    List<Object[]> getCustomerAuditStats(@Param("customerId") UUID customerId);

    /**
     * Get audit statistics for an account
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a " +
           "WHERE a.accountId = :accountId " +
           "GROUP BY a.eventType")
    List<Object[]> getAccountAuditStats(@Param("accountId") UUID accountId);

    /**
     * Get severity distribution
     */
    @Query("SELECT a.severity, COUNT(a) FROM AuditLog a GROUP BY a.severity")
    List<Object[]> getSeverityDistribution();

    /**
     * Get event source distribution
     */
    @Query("SELECT a.eventSource, COUNT(a) FROM AuditLog a GROUP BY a.eventSource")
    List<Object[]> getEventSourceDistribution();

    /**
     * Find recent activity for a customer
     */
    @Query("SELECT a FROM AuditLog a WHERE a.customerId = :customerId " +
           "AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentCustomerActivity(
            @Param("customerId") UUID customerId,
            @Param("since") LocalDateTime since
    );

    /**
     * Find suspicious activity (multiple failed attempts)
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.severity = 'ERROR' AND " +
           "a.eventType IN ('LOGIN_FAILED', 'TRANSACTION_FAILED', 'PAYMENT_FAILED') AND " +
           "a.timestamp >= :since " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findSuspiciousActivity(@Param("since") LocalDateTime since);
}
