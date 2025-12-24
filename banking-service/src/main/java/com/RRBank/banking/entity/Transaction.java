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
 * Transaction Entity
 * Represents money transfers and transactions
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_from_account", columnList = "from_account_id"),
    @Index(name = "idx_transaction_to_account", columnList = "to_account_id"),
    @Index(name = "idx_transaction_reference", columnList = "transaction_reference"),
    @Index(name = "idx_transaction_status", columnList = "status"),
    @Index(name = "idx_transaction_created_at", columnList = "created_at"),
    @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    private String transactionReference;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(name = "from_account_number", length = 20)
    private String fromAccountNumber;

    @Column(name = "to_account_number", length = 20)
    private String toAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "balance_before", precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "fee", precision = 15, scale = 2)
    private BigDecimal fee;

    @Column(name = "merchant_name", length = 100)
    private String merchantName;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_date")
    private LocalDateTime completedDate;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        transactionDate = LocalDateTime.now();
        
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
        if (referenceNumber == null && transactionReference != null) {
            referenceNumber = transactionReference;
        }
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Get transaction ID as string (for DTO compatibility)
     */
    public String getTransactionId() {
        return id != null ? id.toString() : null;
    }

    /**
     * Check if transaction is pending
     */
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }

    /**
     * Check if transaction is completed
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Check if transaction is failed
     */
    public boolean isFailed() {
        return status == TransactionStatus.FAILED || status == TransactionStatus.REVERSED;
    }

    /**
     * Mark transaction as completed
     */
    public void markCompleted() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.completedDate = LocalDateTime.now();
    }

    /**
     * Mark transaction as failed
     */
    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }

    /**
     * Mark transaction as reversed (compensating action)
     */
    public void markReversed(String reason) {
        this.status = TransactionStatus.REVERSED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }

    /**
     * Transaction Type Enum
     */
    public enum TransactionType {
        TRANSFER,       // Internal transfer between accounts
        DEPOSIT,        // Deposit to account
        WITHDRAWAL,     // Withdrawal from account
        PAYMENT,        // Payment transaction
        REFUND,         // Refund transaction
        FEE,            // Fee deduction
        INTEREST        // Interest credit
    }

    /**
     * Transaction Status Enum
     */
    public enum TransactionStatus {
        PENDING,        // Transaction initiated, waiting for processing
        PROCESSING,     // Transaction is being processed
        COMPLETED,      // Transaction successfully completed
        FAILED,         // Transaction failed
        REVERSED,       // Transaction reversed (compensating action)
        CANCELLED       // Transaction cancelled by user
    }
}
