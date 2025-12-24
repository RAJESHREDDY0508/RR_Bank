package com.RRBank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Transaction Statistics DTO
 * Provides summary statistics for an account's transactions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatsDto {

    private UUID accountId;
    private BigDecimal totalOutgoing;
    private BigDecimal totalIncoming;
    private BigDecimal netAmount;
    private Long transactionCount;
}
