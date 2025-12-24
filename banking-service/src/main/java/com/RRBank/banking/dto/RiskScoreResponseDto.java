package com.RRBank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Risk Score Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreResponseDto {

    private UUID transactionId;
    private BigDecimal riskScore;
    private String riskLevel;
    private Boolean isFlagged;
    private String recommendation; // ALLOW, BLOCK, REVIEW
}
