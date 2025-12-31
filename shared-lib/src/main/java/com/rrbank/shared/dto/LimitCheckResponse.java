package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response from limit and fraud check
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitCheckResponse {
    private String decision; // APPROVE, REJECT, REVIEW
    private boolean approved;
    private BigDecimal riskScore;
    private String riskLevel;
    private List<String> reasons;
    private List<String> triggeredRules;
    private BigDecimal remainingDailyLimit;
    private BigDecimal remainingMonthlyLimit;
}
