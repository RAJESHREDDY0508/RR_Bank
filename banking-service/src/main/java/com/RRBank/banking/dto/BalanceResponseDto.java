package com.RRBank.banking.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponseDto {
    private String accountId;
    private String accountNumber;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;

    private String status;
}
