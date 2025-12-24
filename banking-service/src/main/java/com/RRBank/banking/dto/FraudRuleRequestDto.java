package com.RRBank.banking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Create/Update Fraud Rule Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRuleRequestDto {

    @NotBlank(message = "Rule name is required")
    private String ruleName;

    private String ruleDescription;

    @NotBlank(message = "Rule type is required")
    private String ruleType;

    private BigDecimal thresholdValue;

    private Integer timeWindowMinutes;

    @NotNull(message = "Risk score points is required")
    @Min(value = 1, message = "Risk score points must be at least 1")
    private Integer riskScorePoints;

    private Integer priority;

    private Boolean isEnabled;

    private Boolean autoBlock;

    private String countryBlacklist;
}
