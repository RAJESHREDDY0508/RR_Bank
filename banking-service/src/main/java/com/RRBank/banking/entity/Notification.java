package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Entity - User notifications (email, SMS, push, in-app)
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_user", columnList = "user_id"),
    @Index(name = "idx_notifications_type", columnList = "notification_type"),
    @Index(name = "idx_notifications_status", columnList = "status"),
    @Index(name = "idx_notifications_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (priority == null) {
            priority = NotificationPriority.NORMAL;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (isRead == null) {
            isRead = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }

    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsDelivered() {
        this.status = NotificationStatus.DELIVERED;
    }

    public void markAsRead() {
        this.status = NotificationStatus.READ;
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
        this.retryCount++;
    }

    /**
     * Notification Type - Extended for all use cases
     */
    public enum NotificationType {
        // Account notifications
        ACCOUNT_CREATED,
        ACCOUNT_UPDATED,
        ACCOUNT_CLOSED,
        ACCOUNT,
        
        // Balance notifications
        BALANCE_UPDATED,
        BALANCE_LOW,
        
        // Transaction notifications
        TRANSACTION_COMPLETED,
        TRANSACTION_FAILED,
        TRANSACTION,
        
        // Payment notifications
        PAYMENT_SCHEDULED,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT,
        
        // Security notifications
        SECURITY_ALERT,
        SECURITY,
        
        // Other
        MARKETING,
        SYSTEM,
        ALERT
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        DELIVERED,
        READ,
        FAILED
    }

    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
