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

    private UUID transactionId;
    private String transactionReference;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID accountId; // Primary account (for audit compatibility)
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private BigDecimal fromAccountNewBalance;
    private BigDecimal toAccountNewBalance;
    private BigDecimal newBalance; // For audit compatibility
    private String status; // For audit compatibility
    private LocalDateTime completedAt;
    
    @Builder.Default
    private String eventType = "TRANSACTION_COMPLETED";
}
