package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Entity
 * Represents notifications sent to users
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_type", columnList = "notification_type"),
    @Index(name = "idx_notification_channel", columnList = "channel"),
    @Index(name = "idx_notification_status", columnList = "status"),
    @Index(name = "idx_notification_read", columnList = "is_read"),
    @Index(name = "idx_notification_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "reference_id")
    private UUID referenceId; // Transaction ID, Payment ID, etc.

    @Column(name = "reference_type", length = 50)
    private String referenceType; // TRANSACTION, PAYMENT, ACCOUNT, etc.

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON metadata

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
        if (isRead == null) {
            isRead = false;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if notification is pending
     */
    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    /**
     * Check if notification is sent
     */
    public boolean isSent() {
        return status == NotificationStatus.SENT;
    }

    /**
     * Check if notification failed
     */
    public boolean isFailed() {
        return status == NotificationStatus.FAILED;
    }

    /**
     * Mark notification as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Mark notification as failed
     */
    public void markAsFailed(String reason) {
        this.status = NotificationStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
        this.retryCount++;
    }

    /**
     * Mark notification as read
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    /**
     * Notification Type Enum
     */
    public enum NotificationType {
        ACCOUNT_CREATED,
        ACCOUNT_UPDATED,
        ACCOUNT_CLOSED,
        TRANSACTION_COMPLETED,
        TRANSACTION_FAILED,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT_SCHEDULED,
        BALANCE_LOW,
        BALANCE_UPDATED,
        KYC_VERIFIED,
        KYC_REJECTED,
        LOGIN_ALERT,
        SECURITY_ALERT,
        GENERAL
    }

    /**
     * Notification Channel Enum
     */
    public enum NotificationChannel {
        EMAIL,      // Email notification
        SMS,        // SMS notification
        PUSH,       // Push notification (mobile/web)
        IN_APP      // In-app notification
    }

    /**
     * Notification Status Enum
     */
    public enum NotificationStatus {
        PENDING,    // Notification created, waiting to be sent
        SENT,       // Notification sent successfully
        FAILED,     // Notification failed to send
        CANCELLED   // Notification cancelled
    }
}
