package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit Log Entity
 * Immutable audit trail for compliance and tracking
 * APPEND-ONLY: Records cannot be modified or deleted
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_customer", columnList = "customer_id"),
    @Index(name = "idx_audit_account", columnList = "account_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_severity", columnList = "severity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "event_source", nullable = false, length = 100, updatable = false)
    private String eventSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20, updatable = false)
    private Severity severity;

    @Column(name = "entity_type", length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "customer_id", updatable = false)
    private UUID customerId;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String description;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "ip_address", length = 50, updatable = false)
    private String ipAddress;

    @Column(name = "metadata", columnDefinition = "TEXT", updatable = false)
    private String metadata;

    @Column(name = "is_sensitive", nullable = false, updatable = false)
    private Boolean isSensitive;

    @Column(name = "compliance_flag", updatable = false)
    private Boolean complianceFlag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (severity == null) {
            severity = Severity.INFO;
        }
        if (isSensitive == null) {
            isSensitive = false;
        }
        if (complianceFlag == null) {
            complianceFlag = false;
        }
    }

    /**
     * Severity Enum
     */
    public enum Severity {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Check if this is a high-severity event
     */
    public boolean isHighSeverity() {
        return severity == Severity.ERROR || severity == Severity.CRITICAL;
    }

    /**
     * Check if this requires compliance review
     */
    public boolean requiresComplianceReview() {
        return complianceFlag != null && complianceFlag;
    }
}
