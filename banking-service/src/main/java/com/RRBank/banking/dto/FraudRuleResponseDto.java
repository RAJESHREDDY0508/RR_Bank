package com.RRBank.banking.dto;

import com.RRBank.banking.entity.FraudRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fraud Rule Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleResponseDto {

    private UUID id;
    private String ruleName;
    private String ruleDescription;
    private String ruleType;
    private BigDecimal thresholdValue;
    private Integer timeWindowMinutes;
    private Integer riskScorePoints;
    private Integer priority;
    private Boolean isEnabled;
    private Boolean autoBlock;
    private LocalDateTime createdAt;

    public static FraudRuleResponseDto fromEntity(FraudRule rule) {
        return FraudRuleResponseDto.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleDescription(rule.getRuleDescription() != null ? rule.getRuleDescription() : rule.getDescription())
                .ruleType(rule.getRuleType() != null ? rule.getRuleType().name() : null)
                .thresholdValue(rule.getThresholdValue() != null ? rule.getThresholdValue() : rule.getThresholdAmount())
                .timeWindowMinutes(rule.getTimeWindowMinutes())
                .riskScorePoints(rule.getRiskScorePoints())
                .priority(rule.getPriority())
                .isEnabled(rule.getIsEnabled() != null ? rule.getIsEnabled() : rule.getEnabled())
                .autoBlock(rule.getAutoBlock())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
