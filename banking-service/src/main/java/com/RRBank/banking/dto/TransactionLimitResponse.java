package com.RRBank.banking.dto;

import com.RRBank.banking.entity.TransactionLimit;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLimitResponse {
    private UUID id;
    private String userId;
    private String limitType;
    private BigDecimal dailyLimit;
    private BigDecimal perTransactionLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal remainingDaily;
    private BigDecimal remainingMonthly;
    private Boolean enabled;
    
    public static TransactionLimitResponse fromEntity(TransactionLimit limit) {
        return TransactionLimitResponse.builder()
            .id(limit.getId())
            .userId(limit.getUserId())
            .limitType(limit.getLimitType().name())
            .dailyLimit(limit.getDailyLimit())
            .perTransactionLimit(limit.getPerTransactionLimit())
            .monthlyLimit(limit.getMonthlyLimit())
            .remainingDaily(limit.getRemainingDaily())
            .remainingMonthly(limit.getRemainingMonthly())
            .enabled(limit.getEnabled())
            .build();
    }
}
