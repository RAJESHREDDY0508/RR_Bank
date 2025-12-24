package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Entity
 * Represents bill payments and merchant payments
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_account", columnList = "account_id"),
    @Index(name = "idx_payment_customer", columnList = "customer_id"),
    @Index(name = "idx_payment_reference", columnList = "payment_reference"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_type", columnList = "payment_type"),
    @Index(name = "idx_payment_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 50)
    private String paymentReference;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "payee_name", nullable = false, length = 200)
    private String payeeName;

    @Column(name = "payee_account", length = 100)
    private String payeeAccount;

    @Column(name = "payee_reference", length = 100)
    private String payeeReference;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (currency == null) {
            currency = "USD";
        }
        if (scheduledDate == null) {
            scheduledDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if payment is pending
     */
    public boolean isPending() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.SCHEDULED;
    }

    /**
     * Check if payment is completed
     */
    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    /**
     * Check if payment is failed
     */
    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    /**
     * Mark payment as completed
     */
    public void markCompleted(String gatewayTransactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.gatewayTransactionId = gatewayTransactionId;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark payment as failed
     */
    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Check if payment is scheduled for future
     */
    public boolean isScheduledForFuture() {
        return scheduledDate != null && scheduledDate.isAfter(LocalDate.now());
    }

    /**
     * Payment Type Enum
     */
    public enum PaymentType {
        BILL,           // Bill payment (utilities, credit cards, etc.)
        MERCHANT,       // Merchant payment (online shopping, services)
        P2P,            // Person-to-person payment
        SUBSCRIPTION,   // Recurring subscription payment
        INVOICE         // Invoice payment
    }

    /**
     * Payment Status Enum
     */
    public enum PaymentStatus {
        PENDING,        // Payment initiated, waiting for processing
        SCHEDULED,      // Payment scheduled for future date
        PROCESSING,     // Payment is being processed
        COMPLETED,      // Payment successfully completed
        FAILED,         // Payment failed
        CANCELLED,      // Payment cancelled by user
        REFUNDED        // Payment refunded
    }

    /**
     * Payment Method Enum
     */
    public enum PaymentMethod {
        DEBIT_CARD,     // Debit card payment
        CREDIT_CARD,    // Credit card payment
        ACH,            // ACH transfer
        WIRE,           // Wire transfer
        WALLET          // Digital wallet
    }
}
