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
 * Fraud Rule Entity
 * Represents configurable fraud detection rules
 */
@Entity
@Table(name = "fraud_rules", indexes = {
    @Index(name = "idx_fraud_rules_enabled", columnList = "enabled"),
    @Index(name = "idx_fraud_rules_type", columnList = "rule_type"),
    @Index(name = "idx_fraud_rules_priority", columnList = "priority")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_name", nullable = false, unique = true, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Column(name = "rule_description", columnDefinition = "TEXT")
    private String ruleDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "threshold_amount", precision = 15, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "threshold_value", precision = 15, scale = 2)
    private BigDecimal thresholdValue;

    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    @Column(name = "max_frequency")
    private Integer maxFrequency;

    @Column(name = "risk_score_weight", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal riskScoreWeight = BigDecimal.ONE;

    @Column(name = "risk_score_points")
    @Builder.Default
    private Integer riskScorePoints = 10;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "is_enabled")
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "auto_block")
    @Builder.Default
    private Boolean autoBlock = false;

    @Column(name = "country_blacklist", columnDefinition = "TEXT")
    private String countryBlacklist;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
        if (isEnabled == null) isEnabled = true;
        if (priority == null) priority = 0;
        if (riskScoreWeight == null) riskScoreWeight = BigDecimal.ONE;
        if (riskScorePoints == null) riskScorePoints = 10;
        if (autoBlock == null) autoBlock = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Fraud Rule Type Enum
     */
    public enum RuleType {
        HIGH_AMOUNT,           // Transaction exceeds threshold
        TRANSACTION_VELOCITY,  // Too many transactions in time window
        UNUSUAL_LOCATION,      // Transaction from unusual location
        AMOUNT_SPIKE,          // Sudden increase in transaction amounts
        OFF_HOURS,             // Transactions outside normal hours
        ROUND_AMOUNT,          // Suspiciously round amounts
        FOREIGN_TRANSACTION,   // International transactions
        BLACKLISTED_LOCATION,  // Transaction from blacklisted country
        RAPID_SUCCESSION,      // Multiple transactions in quick succession
        AMOUNT_THRESHOLD,      // Generic threshold rule
        FREQUENCY,             // Frequency-based rule
        LOCATION,              // Location-based rule
        TIME,                  // Time-based rule
        DUPLICATE,             // Duplicate detection
        PATTERN,               // Pattern matching
        GEOGRAPHY,             // Geographic rule
        VELOCITY               // Velocity rule
    }

    /**
     * Check if rule is active
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled) || Boolean.TRUE.equals(isEnabled);
    }

    /**
     * Check if should auto-block
     */
    public boolean shouldAutoBlock() {
        return Boolean.TRUE.equals(autoBlock);
    }
}
