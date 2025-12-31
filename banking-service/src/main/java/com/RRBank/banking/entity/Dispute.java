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
 * Dispute Entity
 * Phase 3: Transaction dispute workflow
 */
@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_disputes_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_disputes_account_id", columnList = "account_id"),
    @Index(name = "idx_disputes_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "dispute_number", nullable = false, unique = true, length = 50)
    private String disputeNumber;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "disputed_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal disputedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "dispute_type", nullable = false, length = 30)
    private DisputeType disputeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.SUBMITTED;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "customer_description", columnDefinition = "TEXT")
    private String customerDescription;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "provisional_credit_issued")
    @Builder.Default
    private Boolean provisionalCreditIssued = false;

    @Column(name = "provisional_credit_amount", precision = 19, scale = 4)
    private BigDecimal provisionalCreditAmount;

    @Column(name = "provisional_credit_date")
    private LocalDateTime provisionalCreditDate;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 30)
    private Resolution resolution;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "refund_amount", precision = 19, scale = 4)
    private BigDecimal refundAmount;

    @Column(name = "refund_transaction_id")
    private UUID refundTransactionId;

    @Column(name = "supporting_documents", columnDefinition = "TEXT")
    private String supportingDocuments;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = DisputeStatus.SUBMITTED;
        if (currency == null) currency = "USD";
        if (provisionalCreditIssued == null) provisionalCreditIssued = false;
        if (disputeNumber == null) {
            disputeNumber = "DSP-" + System.currentTimeMillis();
        }
        // Set due date to 45 days from creation (standard dispute resolution period)
        if (dueDate == null) {
            dueDate = now.plusDays(45);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void assignTo(UUID userId) {
        this.assignedTo = userId;
        this.assignedAt = LocalDateTime.now();
        if (this.status == DisputeStatus.SUBMITTED) {
            this.status = DisputeStatus.UNDER_REVIEW;
        }
    }

    public void issueProvisionalCredit(BigDecimal amount) {
        this.provisionalCreditIssued = true;
        this.provisionalCreditAmount = amount;
        this.provisionalCreditDate = LocalDateTime.now();
    }

    public void resolveInCustomerFavor(UUID reviewerId, BigDecimal refundAmt, String notes) {
        this.status = DisputeStatus.RESOLVED;
        this.resolution = Resolution.CUSTOMER_FAVOR;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.refundAmount = refundAmt;
        this.resolutionNotes = notes;
    }

    public void resolveInMerchantFavor(UUID reviewerId, String notes) {
        this.status = DisputeStatus.RESOLVED;
        this.resolution = Resolution.MERCHANT_FAVOR;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    public void reject(UUID reviewerId, String notes) {
        this.status = DisputeStatus.REJECTED;
        this.resolution = Resolution.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }

    public boolean isOpen() {
        return status == DisputeStatus.SUBMITTED || 
               status == DisputeStatus.UNDER_REVIEW || 
               status == DisputeStatus.PENDING_INFO;
    }

    public enum DisputeType {
        UNAUTHORIZED_TRANSACTION,
        DUPLICATE_CHARGE,
        INCORRECT_AMOUNT,
        MERCHANDISE_NOT_RECEIVED,
        SERVICE_NOT_PROVIDED,
        QUALITY_ISSUE,
        CANCELLED_RECURRING,
        CREDIT_NOT_PROCESSED,
        OTHER
    }

    public enum DisputeStatus {
        SUBMITTED,
        UNDER_REVIEW,
        PENDING_INFO,
        ESCALATED,
        RESOLVED,
        REJECTED,
        CLOSED
    }

    public enum Resolution {
        CUSTOMER_FAVOR,
        MERCHANT_FAVOR,
        PARTIAL_REFUND,
        REJECTED,
        WITHDRAWN
    }
}
