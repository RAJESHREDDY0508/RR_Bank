package com.rrbank.shared.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event emitted when fraud is detected
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertEvent {
    private String eventId;
    private String transactionId;
    private String accountId;
    private String userId;
    private BigDecimal amount;
    private BigDecimal riskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> triggeredRules;
    private List<String> reasons;
    private String recommendation; // APPROVE, REVIEW, BLOCK
    private LocalDateTime timestamp;
}
