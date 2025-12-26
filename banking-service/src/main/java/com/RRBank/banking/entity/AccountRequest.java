package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AccountRequest Entity - Account opening requests for admin approval
 */
@Entity
@Table(name = "account_requests", indexes = {
    @Index(name = "idx_request_user", columnList = "user_id"),
    @Index(name = "idx_request_status", columnList = "status"),
    @Index(name = "idx_request_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private Account.AccountType accountType;

    @Column(name = "initial_deposit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "request_notes", columnDefinition = "TEXT")
    private String requestNotes;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "account_id")
    private UUID accountId;

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
    }

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == RequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == RequestStatus.REJECTED;
    }

    public void approve(String adminId, String notes, UUID createdAccountId) {
        this.status = RequestStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = notes;
        this.accountId = createdAccountId;
    }

    public void reject(String adminId, String reason) {
        this.status = RequestStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewedAt = LocalDateTime.now();
        this.adminNotes = reason;
    }

    public void cancel() {
        this.status = RequestStatus.CANCELLED;
    }
}
