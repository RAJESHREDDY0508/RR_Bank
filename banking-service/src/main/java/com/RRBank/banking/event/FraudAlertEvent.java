package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fraud Alert Event
 * Published when high-risk fraud is detected
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertEvent {

    // Fraud identifiers
    private UUID fraudEventId;
    private UUID transactionId;

    // Primary account (used by AuditEventsConsumer)
    private UUID accountId;

    // Domain context
    private UUID customerId;

    // Amounts (domain + audit compatibility)
    private BigDecimal transactionAmount;
    private BigDecimal amount;

    // Risk analysis
    private BigDecimal riskScore;
    private String riskLevel;

    // Required by AuditEventsConsumer
    private String fraudType;

    // Fraud context
    private List<String> fraudReasons;

    // Actions & severity
    private String actionTaken;    // BLOCKED, FLAGGED_FOR_REVIEW
    private String alertSeverity;  // HIGH, CRITICAL

    // Timestamp
    private LocalDateTime alertedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "FRAUD_ALERT";
}
