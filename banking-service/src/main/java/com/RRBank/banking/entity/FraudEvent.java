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
 * Fraud Event Entity - Fraud detection events and alerts
 * 
 * Database constraints:
 * - risk_level: LOW, MEDIUM, HIGH, CRITICAL
 * - risk_score: 0-100
 */
@Entity
@Table(name = "fraud_events", indexes = {
    @Index(name = "idx_fraud_transaction", columnList = "transaction_id"),
    @Index(name = "idx_fraud_account", columnList = "account_id"),
    @Index(name = "idx_fraud_risk_level", columnList = "risk_level"),
    @Index(name = "idx_fraud_resolved", columnList = "resolved"),
    @Index(name = "idx_fraud_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "transaction_amount", precision = 19, scale = 4)
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
    @Builder.Default
    private FraudStatus status = FraudStatus.PENDING_REVIEW;

    @Column(name = "flagged_reason", nullable = false, columnDefinition = "TEXT")
    private String flaggedReason;

    @Column(name = "fraud_reasons", columnDefinition = "TEXT")
    private String fraudReasons;

    @Column(name = "rules_triggered", columnDefinition = "TEXT")
    private String rulesTriggered;

    @Column(name = "details", columnDefinition = "JSONB")
    private String details;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "location_country", length = 100)
    private String locationCountry;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        if (resolved == null) {
            resolved = false;
        }
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
     * Check if event is resolved
     */
    public boolean isResolved() {
        return Boolean.TRUE.equals(resolved);
    }

    /**
     * Mark as resolved
     */
    public void markAsResolved(String resolvedBy, String notes, String action) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
        this.actionTaken = action;
        this.status = FraudStatus.RESOLVED;
    }

    /**
     * Confirm as fraud
     */
    public void confirmAsFraud(String resolvedBy, String notes) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
        this.status = FraudStatus.CONFIRMED_FRAUD;
    }

    /**
     * Mark as false positive
     */
    public void markAsFalsePositive(String resolvedBy, String notes) {
        this.resolved = true;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
        this.status = FraudStatus.FALSE_POSITIVE;
    }

    /**
     * Calculate risk level from risk score
     */
    public static RiskLevel calculateRiskLevel(BigDecimal score) {
        if (score == null) return RiskLevel.LOW;
        
        double scoreValue = score.doubleValue();
        if (scoreValue >= 76) return RiskLevel.CRITICAL;
        if (scoreValue >= 51) return RiskLevel.HIGH;
        if (scoreValue >= 26) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    /**
     * Risk Level - MUST match database constraint
     * Database: CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
     */
    public enum RiskLevel {
        LOW,        // Risk score 0-25
        MEDIUM,     // Risk score 26-50
        HIGH,       // Risk score 51-75
        CRITICAL    // Risk score 76-100
    }

    /**
     * Fraud Event Status
     */
    public enum FraudStatus {
        PENDING_REVIEW,     // Awaiting review
        UNDER_INVESTIGATION, // Being investigated
        CONFIRMED_FRAUD,    // Confirmed as fraud
        FALSE_POSITIVE,     // Determined to be false positive
        RESOLVED            // Issue resolved
    }
}
