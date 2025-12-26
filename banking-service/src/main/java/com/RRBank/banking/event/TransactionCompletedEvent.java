package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Completed Event
 * Published when a transaction completes successfully
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCompletedEvent {

    // Core identifiers
    private UUID transactionId;
    private String transactionReference;

    // Account relationships
    private UUID fromAccountId;
    private UUID toAccountId;

    // Primary account (used by AuditEventsConsumer)
    private UUID accountId;

    // Transaction details
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;

    // Balances
    private BigDecimal fromAccountNewBalance;
    private BigDecimal toAccountNewBalance;

    // Required by AuditEventsConsumer
    private BigDecimal newBalance;
    private String status;

    // Timestamps
    private LocalDateTime completedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "TRANSACTION_COMPLETED";
}
