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
 * Transaction Flagged Event
 * Published when a transaction is flagged as potentially fraudulent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFlaggedEvent {

    // Fraud identifiers
    private UUID fraudEventId;
    private UUID transactionId;

    // Primary account (used by AuditEventsConsumer)
    private UUID accountId;

    // Domain context
    private UUID customerId;
    private BigDecimal transactionAmount;
    private String transactionType;

    // Risk analysis
    private BigDecimal riskScore;
    private String riskLevel;

    // Fraud reasons (domain + audit compatibility)
    private List<String> fraudReasons;
    private String fraudReason;

    // Rules & actions
    private List<String> rulesTriggered;
    private String recommendation; // BLOCK, REVIEW, MONITOR

    // Timestamp
    private LocalDateTime flaggedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "TRANSACTION_FLAGGED";
}
