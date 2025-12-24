package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Created Event
 * Published to Kafka when a new account is created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreatedEvent {

    private UUID accountId;
    private String accountNumber;
    private UUID customerId;
    private String accountType;
    private BigDecimal initialBalance;
    private String currency;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private String eventType = "ACCOUNT_CREATED";
}
