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

    private UUID transactionId;
    private String transactionReference;
    private UUID fromAccountId;
    private UUID toAccountId;
    private UUID accountId; // Primary account (for audit compatibility)
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private String reason; // Alias for failureReason (for audit compatibility)
    private String status; // For audit compatibility
    private LocalDateTime failedAt;
    
    @Builder.Default
    private String eventType = "TRANSACTION_FAILED";
}
