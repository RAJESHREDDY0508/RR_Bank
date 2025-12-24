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

    private UUID fraudEventId;
    private UUID transactionId;
    private UUID accountId;
    private UUID customerId;
    private BigDecimal transactionAmount;
    private String transactionType;
    private BigDecimal riskScore;
    private String riskLevel;
    private List<String> fraudReasons;
    private String fraudReason; // Single reason string for audit compatibility
    private List<String> rulesTriggered;
    private String recommendation; // BLOCK, REVIEW, MONITOR
    private LocalDateTime flaggedAt;
    
    @Builder.Default
    private String eventType = "TRANSACTION_FLAGGED";
}
