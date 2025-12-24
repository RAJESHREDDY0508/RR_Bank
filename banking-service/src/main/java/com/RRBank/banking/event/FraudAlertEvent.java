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

    private UUID fraudEventId;
    private UUID transactionId;
    private UUID accountId;
    private UUID customerId;
    private BigDecimal transactionAmount;
    private BigDecimal amount; // Alias for transactionAmount (for audit compatibility)
    private BigDecimal riskScore;
    private String riskLevel;
    private String fraudType; // For audit compatibility
    private List<String> fraudReasons;
    private String actionTaken; // BLOCKED, FLAGGED_FOR_REVIEW
    private String alertSeverity; // HIGH, CRITICAL
    private LocalDateTime alertedAt;
    
    @Builder.Default
    private String eventType = "FRAUD_ALERT";
}
