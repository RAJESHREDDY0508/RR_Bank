package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Account Response DTO
 * Used for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponseDto {

    private UUID id;
    private String accountNumber;
    private UUID customerId;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private BigDecimal overdraftLimit;
    private BigDecimal interestRate;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Account entity to AccountResponseDto
     */
    public static AccountResponseDto fromEntity(Account account) {
        return AccountResponseDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .overdraftLimit(account.getOverdraftLimit())
                .interestRate(account.getInterestRate())
                .openedAt(account.getOpenedAt())
                .closedAt(account.getClosedAt())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
