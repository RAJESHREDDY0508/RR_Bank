package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event emitted when a transaction fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFailedEvent {
    private String eventId;
    private String transactionId;
    private String type;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String currency;
    private String userId;
    private String failureReason;
    private String errorCode;
    private LocalDateTime failedAt;
}
