package com.rrbank.account.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateAccountRequest {
        @NotNull
        private String userId;
        private String customerId;
        @NotBlank
        private String accountType;
        private String currency;
        private BigDecimal overdraftLimit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccountResponse {
        private String id;
        private String accountNumber;
        private String userId;
        private String customerId;
        private String accountType;
        private String currency;
        private String status;
        private BigDecimal balance;
        private BigDecimal availableBalance;
        private BigDecimal overdraftLimit;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceResponse {
        private String accountId;
        private String accountNumber;
        private BigDecimal balance;
        private BigDecimal availableBalance;
        private String currency;
        private LocalDateTime asOf;
    }
}
