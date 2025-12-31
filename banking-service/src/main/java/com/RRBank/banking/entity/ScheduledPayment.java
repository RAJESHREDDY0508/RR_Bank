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
 * Scheduled Payment Entity
 * Phase 3: Recurring and scheduled transfers/payments
 */
@Entity
@Table(name = "scheduled_payments", indexes = {
    @Index(name = "idx_scheduled_account_id", columnList = "account_id"),
    @Index(name = "idx_scheduled_next_execution", columnList = "next_execution_date"),
    @Index(name = "idx_scheduled_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "schedule_reference", nullable = false, unique = true, length = 50)
    private String scheduleReference;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "payee_id")
    private UUID payeeId;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(name = "to_account_number", length = 50)
    private String toAccountNumber;

    @Column(name = "payee_name", length = 200)
    private String payeeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    private PaymentType paymentType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private Frequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;

    @Column(name = "last_execution_date")
    private LocalDate lastExecutionDate;

    @Column(name = "execution_count")
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "max_executions")
    private Integer maxExecutions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "last_execution_status", length = 30)
    private String lastExecutionStatus;

    @Column(name = "last_execution_error", columnDefinition = "TEXT")
    private String lastExecutionError;

    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @Column(name = "max_consecutive_failures")
    @Builder.Default
    private Integer maxConsecutiveFailures = 3;

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
        if (status == null) status = ScheduleStatus.ACTIVE;
        if (currency == null) currency = "USD";
        if (executionCount == null) executionCount = 0;
        if (consecutiveFailures == null) consecutiveFailures = 0;
        if (maxConsecutiveFailures == null) maxConsecutiveFailures = 3;
        if (nextExecutionDate == null) nextExecutionDate = startDate;
        if (scheduleReference == null) {
            scheduleReference = "SCH-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void recordExecution(boolean success, String error) {
        this.lastExecutionDate = LocalDate.now();
        this.executionCount++;
        
        if (success) {
            this.lastExecutionStatus = "SUCCESS";
            this.lastExecutionError = null;
            this.consecutiveFailures = 0;
            calculateNextExecutionDate();
        } else {
            this.lastExecutionStatus = "FAILED";
            this.lastExecutionError = error;
            this.consecutiveFailures++;
            
            if (consecutiveFailures >= maxConsecutiveFailures) {
                this.status = ScheduleStatus.SUSPENDED;
            }
        }
        
        checkCompletion();
    }

    private void calculateNextExecutionDate() {
        if (lastExecutionDate == null) return;
        
        LocalDate next = switch (frequency) {
            case DAILY -> lastExecutionDate.plusDays(1);
            case WEEKLY -> lastExecutionDate.plusWeeks(1);
            case BIWEEKLY -> lastExecutionDate.plusWeeks(2);
            case MONTHLY -> lastExecutionDate.plusMonths(1);
            case QUARTERLY -> lastExecutionDate.plusMonths(3);
            case YEARLY -> lastExecutionDate.plusYears(1);
            case ONCE -> null;
        };
        
        this.nextExecutionDate = next;
    }

    private void checkCompletion() {
        if (frequency == Frequency.ONCE) {
            this.status = ScheduleStatus.COMPLETED;
        } else if (endDate != null && LocalDate.now().isAfter(endDate)) {
            this.status = ScheduleStatus.COMPLETED;
        } else if (maxExecutions != null && executionCount >= maxExecutions) {
            this.status = ScheduleStatus.COMPLETED;
        }
    }

    public boolean isDueForExecution() {
        if (status != ScheduleStatus.ACTIVE) return false;
        if (nextExecutionDate == null) return false;
        return !LocalDate.now().isBefore(nextExecutionDate);
    }

    public void pause() {
        this.status = ScheduleStatus.PAUSED;
    }

    public void resume() {
        this.status = ScheduleStatus.ACTIVE;
        this.consecutiveFailures = 0;
    }

    public void cancel() {
        this.status = ScheduleStatus.CANCELLED;
    }

    public enum PaymentType {
        TRANSFER,
        BILL_PAYMENT,
        MERCHANT_PAYMENT
    }

    public enum Frequency {
        ONCE,
        DAILY,
        WEEKLY,
        BIWEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY
    }

    public enum ScheduleStatus {
        ACTIVE,
        PAUSED,
        SUSPENDED,
        COMPLETED,
        CANCELLED
    }
}
