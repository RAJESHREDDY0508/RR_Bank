package com.rrbank.admin.service;

import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AdminAuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void logAction(
            UUID adminUserId,
            String adminUsername,
            String action,
            AdminAuditLog.ActionType actionType,
            String entityType,
            String entityId,
            String description,
            String ipAddress,
            String userAgent,
            AdminAuditLog.AuditStatus status,
            String errorMessage
    ) {
        try {
            AdminAuditLog auditLog = AdminAuditLog.builder()
                    .adminUserId(adminUserId)
                    .adminUsername(adminUsername)
                    .action(action)
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} by {}", action, adminUsername);
        } catch (Exception e) {
            log.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    @Transactional
    public AdminAuditLog logActionSync(
            UUID adminUserId,
            String adminUsername,
            String action,
            AdminAuditLog.ActionType actionType,
            String entityType,
            String entityId,
            String description,
            String oldValue,
            String newValue,
            String ipAddress,
            String userAgent
    ) {
        AdminAuditLog auditLog = AdminAuditLog.builder()
                .adminUserId(adminUserId)
                .adminUsername(adminUsername)
                .action(action)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(AdminAuditLog.AuditStatus.SUCCESS)
                .build();

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getAuditLogs(
            UUID adminUserId,
            String action,
            AdminAuditLog.ActionType actionType,
            String entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return auditLogRepository.findAllWithFilters(
                adminUserId,
                action,
                actionType,
                entityType,
                startDate,
                endDate,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLog> getSecurityEvents(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return auditLogRepository.findSecurityEvents(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> getRecentActivity() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getActionCountToday() {
        return auditLogRepository.countSince(LocalDateTime.now().toLocalDate().atStartOfDay());
    }
}
