package com.RRBank.banking.dto;

import com.RRBank.banking.entity.FraudEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fraud Event Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudEventResponseDto {

    private UUID id;
    private UUID transactionId;
    private UUID accountId;
    private UUID customerId;
    private BigDecimal transactionAmount;
    private String transactionType;
    private BigDecimal riskScore;
    private String riskLevel;
    private String status;
    private List<String> fraudReasons;
    private List<String> rulesTriggered;
    private String locationCountry;
    private String locationCity;
    private String actionTaken;
    private LocalDateTime createdAt;

    public static FraudEventResponseDto fromEntity(FraudEvent event) {
        return FraudEventResponseDto.builder()
                .id(event.getId())
                .transactionId(event.getTransactionId())
                .accountId(event.getAccountId())
                .customerId(event.getCustomerId())
                .transactionAmount(event.getTransactionAmount())
                .transactionType(event.getTransactionType())
                .riskScore(event.getRiskScore())
                .riskLevel(event.getRiskLevel().name())
                .status(event.getStatus().name())
                .fraudReasons(event.getFraudReasons() != null ? 
                        List.of(event.getFraudReasons().split(",")) : List.of())
                .rulesTriggered(event.getRulesTriggered() != null ? 
                        List.of(event.getRulesTriggered().split(",")) : List.of())
                .locationCountry(event.getLocationCountry())
                .locationCity(event.getLocationCity())
                .actionTaken(event.getActionTaken())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
