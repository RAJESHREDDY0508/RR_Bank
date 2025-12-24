package com.RRBank.banking.controller;

import com.RRBank.banking.dto.AuditLogResponseDto;
import com.RRBank.banking.dto.AuditSearchDto;
import com.RRBank.banking.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Controller
 * REST API endpoints for audit log management
 * READ-ONLY endpoints - no create/update/delete operations exposed
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    /**
     * Get all audit logs with pagination
     * GET /api/audit/logs?page=0&size=20
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponseDto>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get all audit logs - page: {}, size: {}", page, size);
        Page<AuditLogResponseDto> auditLogs = auditService.getAllAuditLogs(page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a specific customer
     * GET /api/audit/customer/{customerId}?page=0&size=20
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or (hasRole('CUSTOMER') and #customerId == authentication.principal.customerId)")
    public ResponseEntity<Page<AuditLogResponseDto>> getCustomerAuditLogs(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get audit logs for customer: {}", customerId);
        Page<AuditLogResponseDto> auditLogs = auditService.getCustomerAuditLogs(customerId, page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a specific account
     * GET /api/audit/account/{accountId}?page=0&size=20
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('CUSTOMER')")
    public ResponseEntity<Page<AuditLogResponseDto>> getAccountAuditLogs(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get audit logs for account: {}", accountId);
        Page<AuditLogResponseDto> auditLogs = auditService.getAccountAuditLogs(accountId, page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a specific user
     * GET /api/audit/user/{userId}?page=0&size=20
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or (hasRole('CUSTOMER') and #userId == authentication.principal.id)")
    public ResponseEntity<Page<AuditLogResponseDto>> getUserAuditLogs(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get audit logs for user: {}", userId);
        Page<AuditLogResponseDto> auditLogs = auditService.getUserAuditLogs(userId, page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Search audit logs with complex filters
     * POST /api/audit/search
     * Body: AuditSearchDto
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponseDto>> searchAuditLogs(
            @RequestBody AuditSearchDto searchDto
    ) {
        log.info("REST request to search audit logs with filters: {}", searchDto);
        Page<AuditLogResponseDto> auditLogs = auditService.searchAuditLogs(searchDto);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs by event type
     * GET /api/audit/event-type/{eventType}?page=0&size=20
     */
    @GetMapping("/event-type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogsByEventType(
            @PathVariable String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get audit logs for event type: {}", eventType);
        Page<AuditLogResponseDto> auditLogs = auditService.getAuditLogsByEventType(eventType, page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get compliance flagged audit logs
     * GET /api/audit/compliance?page=0&size=20
     */
    @GetMapping("/compliance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponseDto>> getComplianceFlaggedLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get compliance flagged audit logs");
        Page<AuditLogResponseDto> auditLogs = auditService.getComplianceFlaggedLogs(page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get high severity audit logs
     * GET /api/audit/high-severity?page=0&size=20
     */
    @GetMapping("/high-severity")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponseDto>> getHighSeverityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get high severity audit logs");
        Page<AuditLogResponseDto> auditLogs = auditService.getHighSeverityLogs(page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs within date range
     * GET /api/audit/date-range?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59&page=0&size=20
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponseDto>> getAuditLogsInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST request to get audit logs from {} to {}", startDate, endDate);
        Page<AuditLogResponseDto> auditLogs = auditService.getAuditLogsInDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get customer audit statistics
     * GET /api/audit/customer/{customerId}/stats
     */
    @GetMapping("/customer/{customerId}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or (hasRole('CUSTOMER') and #customerId == authentication.principal.customerId)")
    public ResponseEntity<Map<String, Long>> getCustomerAuditStats(@PathVariable UUID customerId) {
        log.info("REST request to get audit statistics for customer: {}", customerId);
        Map<String, Long> stats = auditService.getCustomerAuditStats(customerId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get account audit statistics
     * GET /api/audit/account/{accountId}/stats
     */
    @GetMapping("/account/{accountId}/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Long>> getAccountAuditStats(@PathVariable UUID accountId) {
        log.info("REST request to get audit statistics for account: {}", accountId);
        Map<String, Long> stats = auditService.getAccountAuditStats(accountId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get overall audit statistics
     * GET /api/audit/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> getAuditStatistics() {
        log.info("REST request to get overall audit statistics");
        Map<String, Object> stats = auditService.getAuditStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get recent customer activity (last 24 hours)
     * GET /api/audit/customer/{customerId}/recent
     */
    @GetMapping("/customer/{customerId}/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR') or (hasRole('CUSTOMER') and #customerId == authentication.principal.customerId)")
    public ResponseEntity<List<AuditLogResponseDto>> getRecentCustomerActivity(@PathVariable UUID customerId) {
        log.info("REST request to get recent activity for customer: {}", customerId);
        List<AuditLogResponseDto> auditLogs = auditService.getRecentCustomerActivity(customerId);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get suspicious activity (last 24 hours)
     * GET /api/audit/suspicious
     */
    @GetMapping("/suspicious")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditLogResponseDto>> getSuspiciousActivity() {
        log.info("REST request to get suspicious activity");
        List<AuditLogResponseDto> auditLogs = auditService.getSuspiciousActivity();
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Count audit logs by event type
     * GET /api/audit/count/event-type/{eventType}
     */
    @GetMapping("/count/event-type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Long> countByEventType(@PathVariable String eventType) {
        log.info("REST request to count audit logs by event type: {}", eventType);
        long count = auditService.countByEventType(eventType);
        return ResponseEntity.ok(count);
    }
}
