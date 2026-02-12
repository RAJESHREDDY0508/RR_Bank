package com.rrbank.admin.controller;

import com.rrbank.admin.dto.ManagementDTOs.*;
import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.dto.common.PageResponse;
import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Logs", description = "View admin audit logs and security events")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List audit logs", description = "Get paginated list of audit logs with filters")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID adminUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        AdminAuditLog.ActionType type = null;
        if (actionType != null && !actionType.isEmpty()) {
            try {
                type = AdminAuditLog.ActionType.valueOf(actionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid action type, ignore filter
            }
        }

        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;

        Page<AdminAuditLog> auditLogs = auditLogService.getAuditLogs(
                adminUserId,
                action,
                type,
                entityType,
                start,
                end,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<AuditLogResponse> responses = auditLogs.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                responses,
                page,
                size,
                auditLogs.getTotalElements()
        )));
    }

    @GetMapping("/security-events")
    @Operation(summary = "Get security events", description = "Get login/logout and other security events")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getSecurityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;

        Page<AdminAuditLog> events = auditLogService.getSecurityEvents(
                start,
                end,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<AuditLogResponse> responses = events.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                responses,
                page,
                size,
                events.getTotalElements()
        )));
    }

    @GetMapping("/admin-actions")
    @Operation(summary = "Get admin actions", description = "Get all admin actions (excluding logins)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAdminActions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID adminUserId,
            @RequestParam(required = false) String entityType
    ) {
        Page<AdminAuditLog> actions = auditLogService.getAuditLogs(
                adminUserId,
                null,
                null,
                entityType,
                null,
                null,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<AuditLogResponse> responses = actions.getContent().stream()
                .filter(log -> log.getActionType() != AdminAuditLog.ActionType.LOGIN 
                        && log.getActionType() != AdminAuditLog.ActionType.LOGOUT)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                responses,
                page,
                size,
                actions.getTotalElements()
        )));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log details", description = "Get detailed information about an audit log entry")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLog(@PathVariable UUID id) {
        // In real implementation, fetch from repository
        AuditLogResponse response = AuditLogResponse.builder()
                .id(id)
                .adminUserId(UUID.randomUUID())
                .adminUsername("admin")
                .action("VIEW_CUSTOMER")
                .actionType("VIEW")
                .entityType("CUSTOMER")
                .entityId(UUID.randomUUID().toString())
                .description("Viewed customer details")
                .ipAddress("192.168.1.1")
                .status("SUCCESS")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private AuditLogResponse mapToResponse(AdminAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .adminUserId(log.getAdminUserId())
                .adminUsername(log.getAdminUsername())
                .action(log.getAction())
                .actionType(log.getActionType() != null ? log.getActionType().name() : null)
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
