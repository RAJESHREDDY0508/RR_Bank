package com.rrbank.transaction.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransactionDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepositRequest {
        @NotNull
        private UUID accountId;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        
        private String description;
        private String idempotencyKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WithdrawRequest {
        @NotNull
        private UUID accountId;
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        
        private String description;
        private String idempotencyKey;
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
        
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        
        private String description;
        private String idempotencyKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionResponse {
        private String id;
        private String transactionReference;
        private String fromAccountId;
        private String toAccountId;
        private String transactionType;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String description;
        private String failureReason;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FraudCheckRequest {
        private UUID accountId;
        private UUID userId;
        private String transactionType;
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FraudCheckResponse {
        private String decision;
        private String reason;
        private Integer riskScore;
    }
}
