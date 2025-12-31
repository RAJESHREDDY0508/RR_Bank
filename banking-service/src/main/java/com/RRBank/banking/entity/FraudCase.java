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
 * Fraud Case Entity
 * Phase 3: Fraud review workflow for admin queue
 */
@Entity
@Table(name = "fraud_cases", indexes = {
    @Index(name = "idx_fraud_cases_account_id", columnList = "account_id"),
    @Index(name = "idx_fraud_cases_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_fraud_cases_status", columnList = "status"),
    @Index(name = "idx_fraud_cases_priority", columnList = "priority")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 30)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "detection_method", length = 100)
    private String detectionMethod;

    @Column(name = "fraud_indicators", columnDefinition = "TEXT")
    private String fraudIndicators;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 30)
    private ResolutionType resolutionType;

    @Column(name = "account_action_taken", length = 50)
    private String accountActionTaken;

    @Column(name = "transaction_reversed")
    @Builder.Default
    private Boolean transactionReversed = false;

    @Column(name = "hold_id")
    private UUID holdId;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "escalated")
    @Builder.Default
    private Boolean escalated = false;

    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "escalated_to")
    private UUID escalatedTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = CaseStatus.OPEN;
        if (priority == null) priority = Priority.MEDIUM;
        if (transactionReversed == null) transactionReversed = false;
        if (escalated == null) escalated = false;
        if (caseNumber == null) {
            caseNumber = "FC-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void assignTo(UUID userId) {
        this.assignedTo = userId;
        this.assignedAt = LocalDateTime.now();
        if (this.status == CaseStatus.OPEN) {
            this.status = CaseStatus.UNDER_REVIEW;
        }
    }

    public void approve(UUID reviewerId, String notes) {
        this.status = CaseStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.resolutionType = ResolutionType.APPROVED;
        this.resolution = notes;
        this.closedAt = LocalDateTime.now();
    }

    public void decline(UUID reviewerId, String notes) {
        this.status = CaseStatus.DECLINED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.resolutionType = ResolutionType.DECLINED;
        this.resolution = notes;
        this.closedAt = LocalDateTime.now();
    }

    public void escalate(UUID escalatedToUser) {
        this.escalated = true;
        this.escalatedAt = LocalDateTime.now();
        this.escalatedTo = escalatedToUser;
        this.priority = Priority.HIGH;
    }

    public boolean isOpen() {
        return status == CaseStatus.OPEN || status == CaseStatus.UNDER_REVIEW;
    }

    public boolean isClosed() {
        return status == CaseStatus.APPROVED || status == CaseStatus.DECLINED || status == CaseStatus.CLOSED;
    }

    public enum CaseType {
        SUSPICIOUS_TRANSACTION,
        UNUSUAL_ACTIVITY,
        VELOCITY_BREACH,
        LARGE_TRANSACTION,
        ACCOUNT_TAKEOVER,
        IDENTITY_THEFT,
        MONEY_LAUNDERING,
        UNAUTHORIZED_ACCESS,
        CHARGEBACK,
        OTHER
    }

    public enum CaseStatus {
        OPEN,
        UNDER_REVIEW,
        PENDING_INFO,
        ESCALATED,
        APPROVED,
        DECLINED,
        CLOSED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ResolutionType {
        APPROVED,
        DECLINED,
        FALSE_POSITIVE,
        ACCOUNT_CLOSED,
        TRANSACTION_REVERSED,
        REFERRED_TO_LAW_ENFORCEMENT,
        NO_ACTION_REQUIRED
    }
}
