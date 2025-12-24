package com.RRBank.banking.dto;

import com.RRBank.banking.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit Log Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponseDto {

    private UUID id;
    private LocalDateTime timestamp;
    private String eventType;
    private String eventSource;
    private String severity;
    private String entityType;
    private UUID entityId;
    private UUID userId;
    private UUID customerId;
    private UUID accountId;
    private String action;
    private String description;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String metadata;
    private Boolean isSensitive;
    private Boolean complianceFlag;
    private LocalDateTime createdAt;

    public static AuditLogResponseDto fromEntity(AuditLog log) {
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .eventType(log.getEventType())
                .eventSource(log.getEventSource())
                .severity(log.getSeverity().name())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .userId(log.getUserId())
                .customerId(log.getCustomerId())
                .accountId(log.getAccountId())
                .action(log.getAction())
                .description(log.getDescription())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .metadata(log.getMetadata())
                .isSensitive(log.getIsSensitive())
                .complianceFlag(log.getComplianceFlag())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
