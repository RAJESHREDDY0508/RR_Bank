package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Account;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for account responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private String accountId;                // API-friendly ID
    private String accountNumber;
    private String accountType;

    private BigDecimal balance;
    private BigDecimal availableBalance;
    private String currency;
    private String status;

    private BigDecimal interestRate;
    private BigDecimal overdraftLimit;
    private BigDecimal minimumBalance;

    private LocalDateTime createdAt;
    private LocalDateTime lastStatementDate;

    private List<TransactionResponse> recentTransactions;

    public static AccountResponse fromEntity(Account account) {
        if (account == null) return null;

        return AccountResponse.builder()
                // ✅ FIX: canonical UUID → String
                .accountId(account.getId() != null ? account.getId().toString() : null)

                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType() != null ? account.getAccountType().name() : null)

                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .currency(account.getCurrency())

                .status(account.getStatus() != null ? account.getStatus().name() : null)

                .interestRate(account.getInterestRate())
                .overdraftLimit(account.getOverdraftLimit())
                .minimumBalance(account.getMinimumBalance())

                .createdAt(account.getCreatedAt())
                .lastStatementDate(account.getLastStatementDate())

                .build();
    }
}
