package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Balance response from ledger service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private String accountId;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private long entryCount;
}
