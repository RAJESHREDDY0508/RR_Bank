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
 * Payment Entity - Bill payments and scheduled payments
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_reference", columnList = "payment_reference"),
    @Index(name = "idx_payments_account", columnList = "account_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_scheduled", columnList = "scheduled_date"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 50)
    private String paymentReference;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "payee_name", nullable = false, length = 200)
    private String payeeName;

    @Column(name = "payee_account", length = 50)
    private String payeeAccount;

    @Column(name = "payee_account_number", length = 50)
    private String payeeAccountNumber;

    @Column(name = "payee_reference", length = 100)
    private String payeeReference;

    @Column(name = "payee_bank_code", length = 20)
    private String payeeBankCode;

    @Column(name = "payee_bank_name", length = 100)
    private String payeeBankName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "recurring")
    @Builder.Default
    private Boolean recurring = false;

    @Column(name = "recurring_frequency", length = 20)
    private String recurringFrequency;

    @Column(name = "recurring_end_date")
    private LocalDate recurringEndDate;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        if (status == null) status = PaymentStatus.PENDING;
        if (currency == null) currency = "USD";
        if (recurring == null) recurring = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isScheduled() {
        return status == PaymentStatus.SCHEDULED;
    }

    public boolean isCompleted() {
        return status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public void markProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.executionDate = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
    }

    public void markCompleted(String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.executionDate = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
        this.gatewayTransactionId = transactionId;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.executionDate = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }

    public boolean isScheduledForFuture() {
        return scheduledDate != null && scheduledDate.isAfter(LocalDate.now());
    }

    public boolean isRecurring() {
        return Boolean.TRUE.equals(recurring);
    }

    public enum PaymentType {
        BILL_PAYMENT,
        BILL,
        WIRE_TRANSFER,
        ACH,
        INTERNAL,
        EXTERNAL,
        MERCHANT
    }

    public enum PaymentMethod {
        BANK_TRANSFER,
        DEBIT_CARD,
        CREDIT_CARD,
        ACH,
        WIRE,
        CHECK,
        CASH
    }

    public enum PaymentStatus {
        PENDING,
        SCHEDULED,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
