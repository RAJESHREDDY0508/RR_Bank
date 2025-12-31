package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to create a ledger entry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryRequest {
    private String accountId;
    private String transactionId;
    private String entryType; // CREDIT, DEBIT
    private BigDecimal amount;
    private String currency;
    private String referenceId;
    private String description;
}
