package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TransactionLimit Entity - Per-user transaction limits
 */
@Entity
@Table(name = "transaction_limits", indexes = {
    @Index(name = "idx_limits_user_id", columnList = "user_id"),
    @Index(name = "idx_limits_type", columnList = "limit_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_limit_type", columnNames = {"user_id", "limit_type"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 30)
    private LimitType limitType;

    @Column(name = "daily_limit", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyLimit = new BigDecimal("10000.00");

    @Column(name = "per_transaction_limit", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal perTransactionLimit = new BigDecimal("5000.00");

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyLimit = new BigDecimal("100000.00");

    @Column(name = "remaining_daily", precision = 19, scale = 4)
    private BigDecimal remainingDaily;

    @Column(name = "remaining_monthly", precision = 19, scale = 4)
    private BigDecimal remainingMonthly;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (remainingDaily == null) {
            remainingDaily = dailyLimit;
        }
        if (remainingMonthly == null) {
            remainingMonthly = monthlyLimit;
        }
        if (lastResetDate == null) {
            lastResetDate = LocalDate.now();
        }
    }

    public enum LimitType {
        TRANSFER,
        WITHDRAWAL,
        DEPOSIT,
        PAYMENT,
        ALL
    }

    public boolean needsDailyReset() {
        return lastResetDate == null || !lastResetDate.equals(LocalDate.now());
    }

    public boolean needsMonthlyReset() {
        if (lastResetDate == null) return true;
        LocalDate now = LocalDate.now();
        return lastResetDate.getMonthValue() != now.getMonthValue() 
            || lastResetDate.getYear() != now.getYear();
    }

    public void resetDaily() {
        this.remainingDaily = this.dailyLimit;
        this.lastResetDate = LocalDate.now();
    }

    public void resetMonthly() {
        this.remainingMonthly = this.monthlyLimit;
    }

    public boolean isWithinPerTransactionLimit(BigDecimal amount) {
        return amount.compareTo(perTransactionLimit) <= 0;
    }

    public boolean isWithinDailyLimit(BigDecimal amount) {
        return amount.compareTo(remainingDaily) <= 0;
    }

    public boolean isWithinMonthlyLimit(BigDecimal amount) {
        return remainingMonthly == null || amount.compareTo(remainingMonthly) <= 0;
    }

    public void consumeLimit(BigDecimal amount) {
        if (remainingDaily != null) {
            remainingDaily = remainingDaily.subtract(amount);
        }
        if (remainingMonthly != null) {
            remainingMonthly = remainingMonthly.subtract(amount);
        }
    }
}
