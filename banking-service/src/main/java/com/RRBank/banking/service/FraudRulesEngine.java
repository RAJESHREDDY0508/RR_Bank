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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRulesEngine {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudEventRepository fraudEventRepository;

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
                totalRiskScore += (rule.getRiskScorePoints() != null ? rule.getRiskScorePoints() : 10);
                fraudReasons.add(buildFraudReason(rule, context));
                
                log.warn("Rule triggered: {} for transaction {}", 
                        rule.getRuleName(), context.getTransactionId());
            }
        }

        totalRiskScore = Math.min(totalRiskScore, 100);

        return FraudEvaluationResult.builder()
                .transactionId(context.getTransactionId())
                .riskScore(new BigDecimal(totalRiskScore))
                .triggeredRules(triggeredRules)
                .fraudReasons(fraudReasons)
                .shouldBlock(shouldBlockTransaction(triggeredRules, totalRiskScore))
                .build();
    }

    private boolean evaluateRule(FraudRule rule, TransactionContext context) {
        if (rule.getRuleType() == null) return false;
        
        return switch (rule.getRuleType()) {
            case HIGH_AMOUNT, AMOUNT_THRESHOLD -> evaluateHighAmount(rule, context);
            case TRANSACTION_VELOCITY, VELOCITY, FREQUENCY -> evaluateTransactionVelocity(rule, context);
            case UNUSUAL_LOCATION, LOCATION -> evaluateUnusualLocation(rule, context);
            case AMOUNT_SPIKE, PATTERN -> evaluateAmountSpike(rule, context);
            case OFF_HOURS, TIME -> evaluateOffHours(rule, context);
            case ROUND_AMOUNT, DUPLICATE -> evaluateRoundAmount(rule, context);
            case FOREIGN_TRANSACTION, GEOGRAPHY -> evaluateForeignTransaction(rule, context);
            case BLACKLISTED_LOCATION -> evaluateBlacklistedLocation(rule, context);
            case RAPID_SUCCESSION -> evaluateRapidSuccession(rule, context);
        };
    }

    private boolean evaluateHighAmount(FraudRule rule, TransactionContext context) {
        BigDecimal threshold = rule.getThresholdValue() != null ? 
                rule.getThresholdValue() : rule.getThresholdAmount();
        if (threshold == null) return false;
        return context.getAmount().compareTo(threshold) > 0;
    }

    private boolean evaluateTransactionVelocity(FraudRule rule, TransactionContext context) {
        if (rule.getTimeWindowMinutes() == null) return false;
        
        LocalDateTime since = LocalDateTime.now().minusMinutes(rule.getTimeWindowMinutes());
        long recentTransactionCount = fraudEventRepository.countByAccountIdSince(
                context.getAccountId(), since);
        
        return recentTransactionCount >= 5;
    }

    private boolean evaluateUnusualLocation(FraudRule rule, TransactionContext context) {
        if (context.getLocationCountry() == null) return false;
        return !"US".equals(context.getLocationCountry()) && 
               !"USA".equals(context.getLocationCountry());
    }

    private boolean evaluateAmountSpike(FraudRule rule, TransactionContext context) {
        return context.getAmount().compareTo(new BigDecimal("5000")) > 0;
    }

    private boolean evaluateOffHours(FraudRule rule, TransactionContext context) {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(2, 0)) && now.isBefore(LocalTime.of(6, 0));
    }

    private boolean evaluateRoundAmount(FraudRule rule, TransactionContext context) {
        BigDecimal amount = context.getAmount();
        return amount.stripTrailingZeros().scale() <= 0 && 
               amount.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0 &&
               amount.compareTo(new BigDecimal("1000")) >= 0;
    }

    private boolean evaluateForeignTransaction(FraudRule rule, TransactionContext context) {
        return context.getLocationCountry() != null && 
               !"US".equals(context.getLocationCountry()) &&
               !"USA".equals(context.getLocationCountry());
    }

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

    private boolean evaluateRapidSuccession(FraudRule rule, TransactionContext context) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        long recentCount = fraudEventRepository.countByAccountIdSince(context.getAccountId(), since);
        return recentCount >= 3;
    }

    private boolean shouldBlockTransaction(List<FraudRule> triggeredRules, int riskScore) {
        boolean hasAutoBlock = triggeredRules.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getAutoBlock()));
        boolean isCriticalRisk = riskScore > 75;
        return hasAutoBlock || isCriticalRisk;
    }

    private String buildFraudReason(FraudRule rule, TransactionContext context) {
        if (rule.getRuleType() == null) return rule.getRuleName();
        
        BigDecimal threshold = rule.getThresholdValue() != null ? 
                rule.getThresholdValue() : rule.getThresholdAmount();
        
        return switch (rule.getRuleType()) {
            case HIGH_AMOUNT, AMOUNT_THRESHOLD -> String.format("High amount: $%s exceeds threshold $%s", 
                    context.getAmount(), threshold);
            case TRANSACTION_VELOCITY, VELOCITY, FREQUENCY -> "Too many transactions in short time";
            case UNUSUAL_LOCATION, LOCATION -> String.format("Unusual location: %s", context.getLocationCountry());
            case AMOUNT_SPIKE, PATTERN -> "Transaction amount significantly higher than usual";
            case OFF_HOURS, TIME -> "Transaction during unusual hours (2-6 AM)";
            case ROUND_AMOUNT, DUPLICATE -> "Suspiciously round amount";
            case FOREIGN_TRANSACTION, GEOGRAPHY -> "International transaction";
            case BLACKLISTED_LOCATION -> String.format("Transaction from blacklisted country: %s", 
                    context.getLocationCountry());
            case RAPID_SUCCESSION -> "Multiple transactions in rapid succession";
        };
    }

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
