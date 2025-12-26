package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * VelocityCheck Entity - Transaction velocity monitoring
 */
@Entity
@Table(name = "velocity_checks", indexes = {
    @Index(name = "idx_velocity_user", columnList = "user_id"),
    @Index(name = "idx_velocity_account", columnList = "account_id"),
    @Index(name = "idx_velocity_window", columnList = "window_start")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VelocityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_type", nullable = false, length = 30)
    private CheckType checkType;

    @Column(name = "window_minutes", nullable = false)
    @Builder.Default
    private Integer windowMinutes = 60;

    @Column(name = "max_count", nullable = false)
    @Builder.Default
    private Integer maxCount = 10;

    @Column(name = "current_count")
    @Builder.Default
    private Integer currentCount = 0;

    @Column(name = "window_start")
    private LocalDateTime windowStart;

    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (windowStart == null) windowStart = LocalDateTime.now();
    }

    public enum CheckType {
        TRANSACTION_COUNT,
        AMOUNT_SUM,
        FAILED_COUNT
    }

    public boolean isWindowExpired() {
        return windowStart == null || LocalDateTime.now().isAfter(windowStart.plusMinutes(windowMinutes));
    }

    public boolean isBlocked() {
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }

    public boolean isLimitExceeded() {
        return currentCount >= maxCount;
    }

    public void resetWindow() {
        this.windowStart = LocalDateTime.now();
        this.currentCount = 0;
    }

    public void increment() {
        if (isWindowExpired()) resetWindow();
        this.currentCount++;
        this.lastTransactionAt = LocalDateTime.now();
    }

    public void block(int minutes) {
        this.blockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }
}
