package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event emitted when a transaction is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {
    private String eventId;
    private String transactionId;
    private String type;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;
    private LocalDateTime completedAt;
}
