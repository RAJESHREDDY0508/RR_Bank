package com.RRBank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Balance Response DTO
 * Simplified response for balance queries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponseDto {

    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal availableBalance; // balance + overdraft limit
    private String currency;
    private String status;
}
