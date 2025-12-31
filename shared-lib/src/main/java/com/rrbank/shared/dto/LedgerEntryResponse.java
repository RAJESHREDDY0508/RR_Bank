package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response from ledger entry creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryResponse {
    private String id;
    private String accountId;
    private String transactionId;
    private String entryType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal runningBalance;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
}
