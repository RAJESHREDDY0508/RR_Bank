package com.rrbank.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs", indexes = {
    @Index(name = "idx_admin_audit_user", columnList = "admin_user_id"),
    @Index(name = "idx_admin_audit_action", columnList = "action"),
    @Index(name = "idx_admin_audit_created", columnList = "created_at"),
    @Index(name = "idx_admin_audit_entity", columnList = "entity_type, entity_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "admin_user_id")
    private UUID adminUserId;

    @Column(name = "admin_username", length = 50)
    private String adminUsername;

    @Column(nullable = false, length = 100)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 30)
    @Builder.Default
    private ActionType actionType = ActionType.OTHER;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ActionType {
        LOGIN,
        LOGOUT,
        CREATE,
        UPDATE,
        DELETE,
        VIEW,
        APPROVE,
        REJECT,
        FREEZE,
        UNFREEZE,
        EXPORT,
        OTHER
    }

    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        PARTIAL
    }
}
