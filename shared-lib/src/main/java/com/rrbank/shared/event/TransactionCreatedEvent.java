package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event emitted when a transaction is created
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
    private String eventId;
    private String transactionId;
    private String type; // DEPOSIT, WITHDRAW, TRANSFER
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String status;
    private LocalDateTime timestamp;
}
