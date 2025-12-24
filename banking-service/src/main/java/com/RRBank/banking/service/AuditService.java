package com.RRBank.banking.service;

import com.RRBank.banking.dto.AuditLogResponseDto;
import com.RRBank.banking.dto.AuditSearchDto;
import com.RRBank.banking.entity.AuditLog;
import com.RRBank.banking.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Audit Service
 * Centralized audit logging and compliance tracking
 * IMMUTABLE AUDIT TRAIL - Write-only service (no updates or deletes)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Create an audit log entry
     * This is the ONLY way to write to the audit log
     */
    @Transactional
    public AuditLog createAuditLog(AuditLog auditLog) {
        log.debug("Creating audit log: {}", auditLog.getEventType());
        return auditLogRepository.save(auditLog);
    }

    /**
     * Create audit log with builder pattern
     */
    @Transactional
    public AuditLog createAuditLog(
            String eventType,
            String eventSource,
            AuditLog.Severity severity,
            String entityType,
            UUID entityId,
            UUID userId,
            UUID customerId,
            UUID accountId,
            String action,
            String description,
            String oldValue,
            String newValue,
            String ipAddress,
            String metadata,
            Boolean isSensitive,
            Boolean complianceFlag
    ) {
        AuditLog auditLog = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .eventSource(eventSource)
                .severity(severity)
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .customerId(customerId)
                .accountId(accountId)
                .action(action)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .metadata(metadata)
                .isSensitive(isSensitive)
                .complianceFlag(complianceFlag)
                .build();

        return createAuditLog(auditLog);
    }

    /**
     * Get all audit logs with pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAllAuditLogs(int page, int size) {
        log.debug("Fetching all audit logs - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAll(pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get audit logs for a specific customer
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getCustomerAuditLogs(UUID customerId, int page, int size) {
        log.debug("Fetching audit logs for customer: {}", customerId);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByCustomerIdOrderByTimestampDesc(customerId, pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get audit logs for a specific account
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAccountAuditLogs(UUID accountId, int page, int size) {
        log.debug("Fetching audit logs for account: {}", accountId);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByAccountIdOrderByTimestampDesc(accountId, pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get audit logs for a specific user
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getUserAuditLogs(UUID userId, int page, int size) {
        log.debug("Fetching audit logs for user: {}", userId);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Search audit logs with complex filters
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> searchAuditLogs(AuditSearchDto searchDto) {
        log.debug("Searching audit logs with filters: {}", searchDto);

        int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
        int size = searchDto.getSize() != null ? searchDto.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size);

        // Convert severity strings to enum
        List<AuditLog.Severity> severities = null;
        if (searchDto.getSeverities() != null && !searchDto.getSeverities().isEmpty()) {
            severities = searchDto.getSeverities().stream()
                    .map(AuditLog.Severity::valueOf)
                    .collect(Collectors.toList());
        }

        // Apply high severity filter
        if (Boolean.TRUE.equals(searchDto.getHighSeverityOnly())) {
            severities = Arrays.asList(AuditLog.Severity.ERROR, AuditLog.Severity.CRITICAL);
        }

        return auditLogRepository.searchAuditLogs(
                searchDto.getEventType(),
                searchDto.getEventSource(),
                severities,
                searchDto.getEntityType(),
                searchDto.getEntityId(),
                searchDto.getUserId(),
                searchDto.getCustomerId(),
                searchDto.getAccountId(),
                searchDto.getAction(),
                searchDto.getStartDate(),
                searchDto.getEndDate(),
                searchDto.getComplianceFlagOnly(),
                pageable
        ).map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get compliance flagged audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getComplianceFlaggedLogs(int page, int size) {
        log.debug("Fetching compliance flagged audit logs");
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByComplianceFlagTrueOrderByTimestampDesc(pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get high severity audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getHighSeverityLogs(int page, int size) {
        log.debug("Fetching high severity audit logs");
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findRecentHighSeverityEvents(pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get audit logs by event type
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogsByEventType(String eventType, int page, int size) {
        log.debug("Fetching audit logs for event type: {}", eventType);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType, pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get audit logs within date range
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogsInDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size
    ) {
        log.debug("Fetching audit logs from {} to {}", startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable)
                .map(AuditLogResponseDto::fromEntity);
    }

    /**
     * Get customer audit statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCustomerAuditStats(UUID customerId) {
        log.debug("Fetching audit statistics for customer: {}", customerId);
        List<Object[]> results = auditLogRepository.getCustomerAuditStats(customerId);
        
        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            stats.put((String) result[0], (Long) result[1]);
        }
        return stats;
    }

    /**
     * Get account audit statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getAccountAuditStats(UUID accountId) {
        log.debug("Fetching audit statistics for account: {}", accountId);
        List<Object[]> results = auditLogRepository.getAccountAuditStats(accountId);
        
        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            stats.put((String) result[0], (Long) result[1]);
        }
        return stats;
    }

    /**
     * Get overall audit statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAuditStatistics() {
        log.debug("Fetching overall audit statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Total logs
        stats.put("totalLogs", auditLogRepository.count());
        
        // Severity distribution
        List<Object[]> severityDist = auditLogRepository.getSeverityDistribution();
        Map<String, Long> severityMap = new HashMap<>();
        for (Object[] result : severityDist) {
            severityMap.put(result[0].toString(), (Long) result[1]);
        }
        stats.put("severityDistribution", severityMap);
        
        // Event source distribution
        List<Object[]> sourceDist = auditLogRepository.getEventSourceDistribution();
        Map<String, Long> sourceMap = new HashMap<>();
        for (Object[] result : sourceDist) {
            sourceMap.put((String) result[0], (Long) result[1]);
        }
        stats.put("eventSourceDistribution", sourceMap);
        
        // Compliance flagged count
        stats.put("complianceFlaggedCount", auditLogRepository.countByComplianceFlagTrue());
        
        return stats;
    }

    /**
     * Get recent activity for a customer (last 24 hours)
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getRecentCustomerActivity(UUID customerId) {
        log.debug("Fetching recent activity for customer: {}", customerId);
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return auditLogRepository.findRecentCustomerActivity(customerId, since)
                .stream()
                .map(AuditLogResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get suspicious activity (last 24 hours)
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getSuspiciousActivity() {
        log.debug("Fetching suspicious activity");
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return auditLogRepository.findSuspiciousActivity(since)
                .stream()
                .map(AuditLogResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Count audit logs by event type
     */
    @Transactional(readOnly = true)
    public long countByEventType(String eventType) {
        return auditLogRepository.countByEventType(eventType);
    }

    /**
     * Count audit logs by severity
     */
    @Transactional(readOnly = true)
    public long countBySeverity(AuditLog.Severity severity) {
        return auditLogRepository.countBySeverity(severity);
    }
}
