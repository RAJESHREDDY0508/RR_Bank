package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Failed Event
 * Published when a transaction fails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFailedEvent {

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

    // Failure details
    private String failureReason;

    // Required by AuditEventsConsumer
    private String reason;
    private String status;

    // Timestamp
    private LocalDateTime failedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "TRANSACTION_FAILED";
}
