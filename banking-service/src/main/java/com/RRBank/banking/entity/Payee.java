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
 * Payee / Beneficiary Entity
 * Phase 3: Stores saved payees for transfers and payments
 */
@Entity
@Table(name = "payees", indexes = {
    @Index(name = "idx_payees_customer_id", columnList = "customer_id"),
    @Index(name = "idx_payees_account_number", columnList = "payee_account_number"),
    @Index(name = "idx_payees_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "nickname", nullable = false, length = 100)
    private String nickname;

    @Column(name = "payee_name", nullable = false, length = 200)
    private String payeeName;

    @Column(name = "payee_account_number", nullable = false, length = 50)
    private String payeeAccountNumber;

    @Column(name = "payee_bank_code", length = 20)
    private String payeeBankCode;

    @Column(name = "payee_bank_name", length = 100)
    private String payeeBankName;

    @Column(name = "payee_routing_number", length = 20)
    private String payeeRoutingNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "payee_type", nullable = false, length = 20)
    @Builder.Default
    private PayeeType payeeType = PayeeType.INDIVIDUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PayeeStatus status = PayeeStatus.PENDING_VERIFICATION;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "transfer_limit", precision = 19, scale = 4)
    private BigDecimal transferLimit;

    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = false;

    @Column(name = "internal_account_id")
    private UUID internalAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = PayeeStatus.PENDING_VERIFICATION;
        if (payeeType == null) payeeType = PayeeType.INDIVIDUAL;
        if (isVerified == null) isVerified = false;
        if (isInternal == null) isInternal = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void verify(UUID verifiedByUser) {
        this.isVerified = true;
        this.status = PayeeStatus.ACTIVE;
        this.verifiedAt = LocalDateTime.now();
        this.verifiedBy = verifiedByUser;
    }

    public void deactivate() {
        this.status = PayeeStatus.INACTIVE;
    }

    public void block() {
        this.status = PayeeStatus.BLOCKED;
    }

    public boolean isActive() {
        return status == PayeeStatus.ACTIVE;
    }

    public boolean requiresVerificationForAmount(BigDecimal amount) {
        if (transferLimit == null) return false;
        return amount.compareTo(transferLimit) > 0 && !Boolean.TRUE.equals(isVerified);
    }

    public enum PayeeType {
        INDIVIDUAL,
        BUSINESS,
        UTILITY,
        GOVERNMENT,
        INTERNAL
    }

    public enum PayeeStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        INACTIVE,
        BLOCKED
    }
}
