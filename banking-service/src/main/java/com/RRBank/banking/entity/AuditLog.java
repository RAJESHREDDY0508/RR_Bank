package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit Log Entity - System audit trail for compliance
 * 
 * APPEND-ONLY: Records cannot be modified or deleted.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_service", columnList = "service_name"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_customer", columnList = "customer_id"),
    @Index(name = "idx_audit_account", columnList = "account_id"),
    @Index(name = "idx_audit_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @Column(name = "event_source", length = 50, updatable = false)
    private String eventSource;

    @Column(name = "service_name", length = 50, updatable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20, updatable = false)
    @Builder.Default
    private Severity severity = Severity.INFO;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "customer_id", updatable = false)
    private UUID customerId;

    @Column(name = "account_id", updatable = false)
    private UUID accountId;

    @Column(name = "transaction_id", updatable = false)
    private UUID transactionId;

    @Column(name = "entity_type", length = 50, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "description", columnDefinition = "TEXT", updatable = false)
    private String description;

    @Column(name = "old_value", columnDefinition = "JSONB", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "JSONB", updatable = false)
    private String newValue;

    @Column(name = "ip_address", length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT", updatable = false)
    private String userAgent;

    @Column(name = "request_id", length = 36, updatable = false)
    private String requestId;

    @Column(name = "metadata", columnDefinition = "JSONB", updatable = false)
    private String metadata;

    @Column(name = "is_sensitive", updatable = false)
    @Builder.Default
    private Boolean isSensitive = false;

    @Column(name = "compliance_flag", updatable = false)
    @Builder.Default
    private Boolean complianceFlag = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, updatable = false)
    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;

    @Column(name = "error_message", columnDefinition = "TEXT", updatable = false)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        
        if (timestamp == null) {
            timestamp = now;
        }
        if (severity == null) {
            severity = Severity.INFO;
        }
        if (status == null) {
            status = AuditStatus.SUCCESS;
        }
        if (isSensitive == null) {
            isSensitive = false;
        }
        if (complianceFlag == null) {
            complianceFlag = false;
        }
    }

    /**
     * Severity Level for audit events
     */
    public enum Severity {
        DEBUG,      // Debug level events
        INFO,       // Informational events
        WARNING,    // Warning events
        ERROR,      // Error events
        CRITICAL    // Critical events requiring immediate attention
    }

    /**
     * Audit Status
     */
    public enum AuditStatus {
        SUCCESS,    // Operation completed successfully
        FAILURE,    // Operation failed
        PARTIAL     // Operation partially completed
    }

    /**
     * Check if this is a failed operation
     */
    public boolean isFailed() {
        return status == AuditStatus.FAILURE;
    }

    /**
     * Check if this is a successful operation
     */
    public boolean isSuccessful() {
        return status == AuditStatus.SUCCESS;
    }

    /**
     * Check if this is a high severity event
     */
    public boolean isHighSeverity() {
        return severity == Severity.ERROR || severity == Severity.CRITICAL;
    }
}
