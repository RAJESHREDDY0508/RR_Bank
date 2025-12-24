package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Balance Updated Event
 * Published to Kafka when account balance is updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceUpdatedEvent {

    private UUID accountId;
    private String accountNumber;
    private UUID customerId;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private BigDecimal changeAmount;
    private String transactionType; // CREDIT, DEBIT
    private String transactionId; // Reference to transaction
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private String eventType = "BALANCE_UPDATED";
}
