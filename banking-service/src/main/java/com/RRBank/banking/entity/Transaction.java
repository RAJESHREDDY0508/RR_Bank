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
 * Transaction Entity - Financial transactions
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transactions_reference", columnList = "transaction_reference"),
    @Index(name = "idx_transactions_from_account", columnList = "from_account_id"),
    @Index(name = "idx_transactions_to_account", columnList = "to_account_id"),
    @Index(name = "idx_transactions_status", columnList = "status"),
    @Index(name = "idx_transactions_type", columnList = "transaction_type"),
    @Index(name = "idx_transactions_created_at", columnList = "created_at"),
    @Index(name = "idx_transactions_idempotency", columnList = "idempotency_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    private String transactionReference;

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "from_account_number", length = 50)
    private String fromAccountNumber;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(name = "to_account_number", length = 50)
    private String toAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "fee_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "balance_before", precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        transactionDate = now;
        
        if (status == null) status = TransactionStatus.PENDING;
        if (currency == null) currency = "USD";
        if (feeAmount == null) feeAmount = BigDecimal.ZERO;
        if (fee == null) fee = BigDecimal.ZERO;
        if (exchangeRate == null) exchangeRate = BigDecimal.ONE;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get transaction ID as string
     */
    public String getTransactionId() {
        return id != null ? id.toString() : null;
    }

    /**
     * Get reference number (alias for transactionReference)
     */
    public String getReferenceNumber() {
        return transactionReference;
    }

    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == TransactionStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == TransactionStatus.CANCELLED;
    }

    public boolean isReversed() {
        return status == TransactionStatus.REVERSED;
    }

    public void markProcessing() {
        this.status = TransactionStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedDate = this.completedAt;
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }

    public void markCancelled(String reason) {
        this.status = TransactionStatus.CANCELLED;
        this.failureReason = reason;
    }

    public void markReversed(String reason) {
        this.status = TransactionStatus.REVERSED;
        this.failureReason = reason;
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER,
        TRANSFER_OUT,  // Phase 2C.2: Debit side of transfer
        TRANSFER_IN,   // Phase 2C.2: Credit side of transfer
        PAYMENT,
        FEE,
        INTEREST,
        REFUND,
        ADJUSTMENT
    }

    public enum TransactionStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REVERSED
    }
}
