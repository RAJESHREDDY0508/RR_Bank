package com.rrbank.fraud.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

public class FraudDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FraudCheckRequest {
        private UUID accountId;
        private UUID userId;
        private String transactionType;
        @NotNull
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserLimitsResponse {
        private String userId;
        private BigDecimal dailyLimit;
        private BigDecimal dailyUsed;
        private BigDecimal remainingDaily;
        private Integer maxWithdrawalsPerHour;
        private Integer withdrawalsThisHour;
    }
}
