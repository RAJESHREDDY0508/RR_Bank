package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Status Changed Event
 * Published to Kafka when account status is changed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatusChangedEvent {

    private UUID accountId;
    private String accountNumber;
    private UUID customerId;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private String changedBy; // User who made the change
    private LocalDateTime changedAt;
    
    @Builder.Default
    private String eventType = "ACCOUNT_STATUS_CHANGED";
}
