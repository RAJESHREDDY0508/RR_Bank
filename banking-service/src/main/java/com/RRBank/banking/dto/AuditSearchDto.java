package com.RRBank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Audit Search DTO
 * Used for filtering audit logs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSearchDto {

    private String eventType;
    private String eventSource;
    private List<String> severities;
    private String entityType;
    private UUID entityId;
    private UUID userId;
    private UUID customerId;
    private UUID accountId;
    private String action;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean complianceFlagOnly;
    private Boolean highSeverityOnly;
    private Integer page;
    private Integer size;
}
