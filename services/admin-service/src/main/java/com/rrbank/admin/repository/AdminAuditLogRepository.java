package com.rrbank.admin.repository;

import com.rrbank.admin.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    Page<AdminAuditLog> findByAdminUserIdOrderByCreatedAtDesc(UUID adminUserId, Pageable pageable);

    @Query("SELECT a FROM AdminAuditLog a WHERE " +
           "(:adminUserId IS NULL OR a.adminUserId = :adminUserId) " +
           "AND (:action IS NULL OR a.action LIKE CONCAT('%', :action, '%')) " +
           "AND (:actionType IS NULL OR a.actionType = :actionType) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findAllWithFilters(
            @Param("adminUserId") UUID adminUserId,
            @Param("action") String action,
            @Param("actionType") AdminAuditLog.ActionType actionType,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT a FROM AdminAuditLog a WHERE a.actionType IN ('LOGIN', 'LOGOUT') " +
           "AND (:startDate IS NULL OR a.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AdminAuditLog> findSecurityEvents(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    List<AdminAuditLog> findTop20ByOrderByCreatedAtDesc();

    long countByActionType(AdminAuditLog.ActionType actionType);

    @Query("SELECT COUNT(a) FROM AdminAuditLog a WHERE a.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);
}
