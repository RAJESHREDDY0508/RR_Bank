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
 * Fraud Rule Entity - Configurable fraud detection rules
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
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rule_name", nullable = false, unique = true, length = 100)
    private String ruleName;

    @Column(name = "rule_description", columnDefinition = "TEXT")
    private String ruleDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "threshold_amount", precision = 19, scale = 4)
    private BigDecimal thresholdAmount;

    @Column(name = "threshold_value", precision = 19, scale = 4)
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        
        if (enabled == null) enabled = true;
        if (isEnabled == null) isEnabled = true;
        if (autoBlock == null) autoBlock = false;
        if (priority == null) priority = 0;
        if (riskScoreWeight == null) riskScoreWeight = BigDecimal.ONE;
        if (riskScorePoints == null) riskScorePoints = 10;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(enabled) || Boolean.TRUE.equals(isEnabled);
    }

    public enum RuleType {
        AMOUNT_THRESHOLD,
        FREQUENCY,
        LOCATION,
        TIME,
        DUPLICATE,
        PATTERN,
        GEOGRAPHY,
        VELOCITY,
        HIGH_AMOUNT,
        TRANSACTION_VELOCITY,
        UNUSUAL_LOCATION,
        AMOUNT_SPIKE,
        OFF_HOURS,
        ROUND_AMOUNT,
        FOREIGN_TRANSACTION,
        BLACKLISTED_LOCATION,
        RAPID_SUCCESSION
    }
}
