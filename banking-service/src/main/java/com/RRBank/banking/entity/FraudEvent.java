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
 * Fraud Event Entity
 * Represents detected fraud events and flagged transactions
 */
@Entity
@Table(name = "fraud_events", indexes = {
    @Index(name = "idx_fraud_transaction", columnList = "transaction_id"),
    @Index(name = "idx_fraud_account", columnList = "account_id"),
    @Index(name = "idx_fraud_status", columnList = "status"),
    @Index(name = "idx_fraud_risk_level", columnList = "risk_level"),
    @Index(name = "idx_fraud_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "transaction_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FraudStatus status;

    @Column(name = "fraud_reasons", columnDefinition = "TEXT")
    private String fraudReasons; // Comma-separated reasons

    @Column(name = "rules_triggered", columnDefinition = "TEXT")
    private String rulesTriggered; // Comma-separated rule IDs

    @Column(name = "location_ip", length = 50)
    private String locationIp;

    @Column(name = "location_country", length = 100)
    private String locationCountry;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "device_fingerprint", length = 200)
    private String deviceFingerprint;

    @Column(name = "transaction_velocity", precision = 10, scale = 2)
    private BigDecimal transactionVelocity; // Transactions per hour

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "action_taken", length = 50)
    private String actionTaken; // BLOCKED, ALLOWED, FLAGGED_FOR_REVIEW

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (status == null) {
            status = FraudStatus.PENDING_REVIEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if event is high risk
     */
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    /**
     * Check if event is pending review
     */
    public boolean isPendingReview() {
        return status == FraudStatus.PENDING_REVIEW;
    }

    /**
     * Mark as reviewed
     */
    public void markAsReviewed(UUID reviewerId, String notes, String action) {
        this.reviewedBy = reviewerId;
        this.reviewedAt = LocalDateTime.now();
        this.reviewNotes = notes;
        this.actionTaken = action;
        this.status = FraudStatus.REVIEWED;
    }

    /**
     * Mark as confirmed fraud
     */
    public void markAsConfirmedFraud() {
        this.status = FraudStatus.CONFIRMED_FRAUD;
        this.actionTaken = "BLOCKED";
    }

    /**
     * Mark as false positive
     */
    public void markAsFalsePositive() {
        this.status = FraudStatus.FALSE_POSITIVE;
        this.actionTaken = "ALLOWED";
    }

    /**
     * Risk Level Enum
     */
    public enum RiskLevel {
        LOW,        // Risk score 0-25
        MEDIUM,     // Risk score 26-50
        HIGH,       // Risk score 51-75
        CRITICAL    // Risk score 76-100
    }

    /**
     * Fraud Status Enum
     */
    public enum FraudStatus {
        PENDING_REVIEW,     // Flagged, waiting for review
        UNDER_INVESTIGATION,// Being investigated
        REVIEWED,           // Reviewed by fraud analyst
        CONFIRMED_FRAUD,    // Confirmed as fraud
        FALSE_POSITIVE,     // Not fraud (false alarm)
        AUTO_CLEARED        // Automatically cleared (low risk)
    }
}
