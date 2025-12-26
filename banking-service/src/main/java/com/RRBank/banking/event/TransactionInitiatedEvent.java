package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Initiated Event
 * Published when a transaction is initiated
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInitiatedEvent {

    // Core identifiers
    private UUID transactionId;
    private UUID accountId;
    private UUID fromAccountId;
    private UUID toAccountId;

    // Transaction details
    private BigDecimal amount;
    private String transactionType;
    private String currency;
    private String description;

    // Status & tracking
    private String status;
    private String transactionReference;

    // Audit metadata
    private UUID initiatedBy;
    private LocalDateTime initiatedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "TRANSACTION_INITIATED";
}
