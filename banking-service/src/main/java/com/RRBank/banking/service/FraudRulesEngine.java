package com.RRBank.banking.service;

import com.RRBank.banking.entity.FraudRule;
import com.RRBank.banking.repository.FraudEventRepository;
import com.RRBank.banking.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fraud Rules Engine
 * Evaluates transactions against configured fraud detection rules
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRulesEngine {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudEventRepository fraudEventRepository;

    /**
     * Evaluate transaction against all enabled fraud rules
     * Returns list of triggered rules
     */
    public FraudEvaluationResult evaluateTransaction(TransactionContext context) {
        log.info("Evaluating transaction {} for fraud", context.getTransactionId());

        List<FraudRule> enabledRules = fraudRuleRepository.findAllEnabledOrderByPriority();
        List<FraudRule> triggeredRules = new ArrayList<>();
        List<String> fraudReasons = new ArrayList<>();
        int totalRiskScore = 0;

        for (FraudRule rule : enabledRules) {
            boolean triggered = evaluateRule(rule, context);
            
            if (triggered) {
                triggeredRules.add(rule);
                totalRiskScore += rule.getRiskScorePoints();
                fraudReasons.add(buildFraudReason(rule, context));
                
                log.warn("Rule triggered: {} for transaction {}", 
                        rule.getRuleName(), context.getTransactionId());
            }
        }

        // Cap risk score at 100
        totalRiskScore = Math.min(totalRiskScore, 100);

        return FraudEvaluationResult.builder()
                .transactionId(context.getTransactionId())
                .riskScore(new BigDecimal(totalRiskScore))
                .triggeredRules(triggeredRules)
                .fraudReasons(fraudReasons)
                .shouldBlock(shouldBlockTransaction(triggeredRules, totalRiskScore))
                .build();
    }

    /**
     * Evaluate individual rule
     */
    private boolean evaluateRule(FraudRule rule, TransactionContext context) {
        return switch (rule.getRuleType()) {
            case HIGH_AMOUNT -> evaluateHighAmount(rule, context);
            case TRANSACTION_VELOCITY -> evaluateTransactionVelocity(rule, context);
            case UNUSUAL_LOCATION -> evaluateUnusualLocation(rule, context);
            case AMOUNT_SPIKE -> evaluateAmountSpike(rule, context);
            case OFF_HOURS -> evaluateOffHours(rule, context);
            case ROUND_AMOUNT -> evaluateRoundAmount(rule, context);
            case FOREIGN_TRANSACTION -> evaluateForeignTransaction(rule, context);
            case BLACKLISTED_LOCATION -> evaluateBlacklistedLocation(rule, context);
            case RAPID_SUCCESSION -> evaluateRapidSuccession(rule, context);
            default -> {
                log.warn("Unknown rule type: {}", rule.getRuleType());
                yield false;
            }
        };
    }

    /**
     * HIGH_AMOUNT rule: Transaction amount exceeds threshold
     */
    private boolean evaluateHighAmount(FraudRule rule, TransactionContext context) {
        if (rule.getThresholdValue() == null) return false;
        return context.getAmount().compareTo(rule.getThresholdValue()) > 0;
    }

    /**
     * TRANSACTION_VELOCITY rule: Too many transactions in short time
     */
    private boolean evaluateTransactionVelocity(FraudRule rule, TransactionContext context) {
        if (rule.getTimeWindowMinutes() == null) return false;
        
        LocalDateTime since = LocalDateTime.now().minusMinutes(rule.getTimeWindowMinutes());
        long recentTransactionCount = fraudEventRepository.countByAccountIdSince(
                context.getAccountId(), since);
        
        // More than 5 transactions in the time window
        return recentTransactionCount >= 5;
    }

    /**
     * UNUSUAL_LOCATION rule: Transaction from unusual country
     */
    private boolean evaluateUnusualLocation(FraudRule rule, TransactionContext context) {
        if (context.getLocationCountry() == null) return false;
        
        // Check if location is different from user's typical location
        // In production, would compare against user's location history
        // For now, flag if country is not US
        return !"US".equals(context.getLocationCountry()) && 
               !"USA".equals(context.getLocationCountry());
    }

    /**
     * AMOUNT_SPIKE rule: Sudden increase in transaction amount
     */
    private boolean evaluateAmountSpike(FraudRule rule, TransactionContext context) {
        // Check if this transaction is 5x larger than user's average
        // In production, would calculate from transaction history
        // For now, flag amounts over $5000
        return context.getAmount().compareTo(new BigDecimal("5000")) > 0;
    }

    /**
     * OFF_HOURS rule: Transaction during unusual hours (2 AM - 6 AM)
     */
    private boolean evaluateOffHours(FraudRule rule, TransactionContext context) {
        LocalTime now = LocalTime.now();
        LocalTime startOffHours = LocalTime.of(2, 0);
        LocalTime endOffHours = LocalTime.of(6, 0);
        
        return now.isAfter(startOffHours) && now.isBefore(endOffHours);
    }

    /**
     * ROUND_AMOUNT rule: Suspiciously round amounts
     */
    private boolean evaluateRoundAmount(FraudRule rule, TransactionContext context) {
        // Check if amount is a round number (e.g., $1000.00, $5000.00)
        BigDecimal amount = context.getAmount();
        
        // If amount has no decimal places and is divisible by 1000
        return amount.stripTrailingZeros().scale() <= 0 && 
               amount.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0 &&
               amount.compareTo(new BigDecimal("1000")) >= 0;
    }

    /**
     * FOREIGN_TRANSACTION rule: International transaction
     */
    private boolean evaluateForeignTransaction(FraudRule rule, TransactionContext context) {
        return context.getLocationCountry() != null && 
               !"US".equals(context.getLocationCountry()) &&
               !"USA".equals(context.getLocationCountry());
    }

    /**
     * BLACKLISTED_LOCATION rule: Transaction from blacklisted country
     */
    private boolean evaluateBlacklistedLocation(FraudRule rule, TransactionContext context) {
        if (rule.getCountryBlacklist() == null || context.getLocationCountry() == null) {
            return false;
        }
        
        String[] blacklistedCountries = rule.getCountryBlacklist().split(",");
        for (String country : blacklistedCountries) {
            if (country.trim().equalsIgnoreCase(context.getLocationCountry())) {
                return true;
            }
        }
        return false;
    }

    /**
     * RAPID_SUCCESSION rule: Multiple transactions within minutes
     */
    private boolean evaluateRapidSuccession(FraudRule rule, TransactionContext context) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        long recentCount = fraudEventRepository.countByAccountIdSince(
                context.getAccountId(), since);
        
        // More than 3 transactions in 5 minutes
        return recentCount >= 3;
    }

    /**
     * Determine if transaction should be automatically blocked
     */
    private boolean shouldBlockTransaction(List<FraudRule> triggeredRules, int riskScore) {
        // Block if any triggered rule has autoBlock enabled
        boolean hasAutoBlock = triggeredRules.stream()
                .anyMatch(FraudRule::getAutoBlock);
        
        // Or block if risk score is critically high (>75)
        boolean isCriticalRisk = riskScore > 75;
        
        return hasAutoBlock || isCriticalRisk;
    }

    /**
     * Build human-readable fraud reason
     */
    private String buildFraudReason(FraudRule rule, TransactionContext context) {
        return switch (rule.getRuleType()) {
            case HIGH_AMOUNT -> String.format("High amount: $%s exceeds threshold $%s", 
                    context.getAmount(), rule.getThresholdValue());
            case TRANSACTION_VELOCITY -> "Too many transactions in short time";
            case UNUSUAL_LOCATION -> String.format("Unusual location: %s", context.getLocationCountry());
            case AMOUNT_SPIKE -> "Transaction amount significantly higher than usual";
            case OFF_HOURS -> "Transaction during unusual hours (2-6 AM)";
            case ROUND_AMOUNT -> "Suspiciously round amount";
            case FOREIGN_TRANSACTION -> "International transaction";
            case BLACKLISTED_LOCATION -> String.format("Transaction from blacklisted country: %s", 
                    context.getLocationCountry());
            case RAPID_SUCCESSION -> "Multiple transactions in rapid succession";
            default -> rule.getRuleName();
        };
    }

    /**
     * Transaction Context - Data passed to rules engine
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TransactionContext {
        private UUID transactionId;
        private UUID accountId;
        private UUID customerId;
        private BigDecimal amount;
        private String transactionType;
        private String locationCountry;
        private String locationCity;
        private String locationIp;
        private String deviceFingerprint;
    }

    /**
     * Fraud Evaluation Result
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FraudEvaluationResult {
        private UUID transactionId;
        private BigDecimal riskScore;
        private List<FraudRule> triggeredRules;
        private List<String> fraudReasons;
        private boolean shouldBlock;
    }
}
