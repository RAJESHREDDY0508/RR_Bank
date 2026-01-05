package com.rrbank.ledger.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LedgerDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateEntryRequest {
        @NotNull
        private UUID accountId;
        
        private UUID transactionId;
        
        @NotNull
        private String entryType; // DEBIT or CREDIT
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LedgerEntryResponse {
        private String id;
        private String accountId;
        private String transactionId;
        private String entryType;
        private BigDecimal amount;
        private BigDecimal runningBalance;
        private String description;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BalanceResponse {
        private String accountId;
        private BigDecimal balance;
        private LocalDateTime asOf;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferRequest {
        @NotNull
        private UUID fromAccountId;
        
        @NotNull
        private UUID toAccountId;
        
        private UUID transactionId;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        
        private String description;
    }
}
