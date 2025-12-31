package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event emitted when a ledger entry is created
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryCreatedEvent {
    private String eventId;
    private String entryId;
    private String accountId;
    private String entryType; // CREDIT, DEBIT
    private BigDecimal amount;
    private BigDecimal runningBalance;
    private String transactionId;
    private String referenceId;
    private String description;
    private LocalDateTime timestamp;
}
