package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Hold Entity
 * Phase 3: Holds on account balances (pending transactions, fraud review, etc.)
 */
@Entity
@Table(name = "holds", indexes = {
    @Index(name = "idx_holds_account_id", columnList = "account_id"),
    @Index(name = "idx_holds_status", columnList = "status"),
    @Index(name = "idx_holds_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "hold_type", nullable = false, length = 30)
    private HoldType holdType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private HoldStatus status = HoldStatus.ACTIVE;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "release_reason", columnDefinition = "TEXT")
    private String releaseReason;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = HoldStatus.ACTIVE;
        if (currency == null) currency = "USD";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == HoldStatus.ACTIVE;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public void release(UUID releasedByUser, String reason) {
        this.status = HoldStatus.RELEASED;
        this.releasedAt = LocalDateTime.now();
        this.releasedBy = releasedByUser;
        this.releaseReason = reason;
    }

    public void expire() {
        this.status = HoldStatus.EXPIRED;
        this.releasedAt = LocalDateTime.now();
        this.releaseReason = "Hold expired";
    }

    public void capture() {
        this.status = HoldStatus.CAPTURED;
        this.releasedAt = LocalDateTime.now();
    }

    public enum HoldType {
        PENDING_TRANSACTION,
        FRAUD_REVIEW,
        DISPUTE,
        REGULATORY,
        MERCHANT_AUTHORIZATION,
        CHECK_DEPOSIT,
        WIRE_TRANSFER,
        ACH_TRANSFER,
        ADMIN_HOLD
    }

    public enum HoldStatus {
        ACTIVE,
        RELEASED,
        EXPIRED,
        CAPTURED
    }
}
