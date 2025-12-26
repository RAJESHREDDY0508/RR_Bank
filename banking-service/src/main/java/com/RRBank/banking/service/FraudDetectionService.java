package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.FraudEvent;
import com.RRBank.banking.entity.FraudRule;
import com.RRBank.banking.event.FraudAlertEvent;
import com.RRBank.banking.event.TransactionFlaggedEvent;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.FraudEventRepository;
import com.RRBank.banking.repository.FraudRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fraud Detection Service
 */
@Service
@Slf4j
public class FraudDetectionService {

    private final FraudRulesEngine rulesEngine;
    private final FraudEventRepository fraudEventRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudEventProducer eventProducer;

    @Autowired
    public FraudDetectionService(
            FraudRulesEngine rulesEngine,
            FraudEventRepository fraudEventRepository,
            FraudRuleRepository fraudRuleRepository,
            @Autowired(required = false) FraudEventProducer eventProducer) {
        this.rulesEngine = rulesEngine;
        this.fraudEventRepository = fraudEventRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public FraudEvent analyzeTransaction(FraudRulesEngine.TransactionContext context) {
        log.info("Analyzing transaction {} for fraud", context.getTransactionId());

        FraudRulesEngine.FraudEvaluationResult result = rulesEngine.evaluateTransaction(context);
        FraudEvent.RiskLevel riskLevel = calculateRiskLevel(result.getRiskScore());

        FraudEvent fraudEvent = FraudEvent.builder()
                .transactionId(context.getTransactionId())
                .accountId(context.getAccountId())
                .customerId(context.getCustomerId())
                .transactionAmount(context.getAmount())
                .transactionType(context.getTransactionType())
                .riskScore(result.getRiskScore())
                .riskLevel(riskLevel)
                .status(FraudEvent.FraudStatus.PENDING_REVIEW)
                .flaggedReason(String.join(", ", result.getFraudReasons()))
                .fraudReasons(String.join(",", result.getFraudReasons()))
                .rulesTriggered(result.getTriggeredRules().stream()
                        .map(r -> r.getId().toString())
                        .collect(Collectors.joining(",")))
                .locationCountry(context.getLocationCountry())
                .locationCity(context.getLocationCity())
                .ipAddress(context.getLocationIp())
                .deviceFingerprint(context.getDeviceFingerprint())
                .actionTaken(result.isShouldBlock() ? "BLOCKED" : "FLAGGED_FOR_REVIEW")
                .eventType("TRANSACTION_FRAUD_CHECK")
                .build();

        fraudEvent = fraudEventRepository.save(fraudEvent);
        publishEvents(fraudEvent, result);

        log.info("Fraud analysis complete for transaction {}. Risk score: {}, Level: {}",
                context.getTransactionId(), result.getRiskScore(), riskLevel);

        return fraudEvent;
    }

    @Transactional(readOnly = true)
    public RiskScoreResponseDto getRiskScore(UUID transactionId) {
        log.info("Getting risk score for transaction: {}", transactionId);

        FraudEvent fraudEvent = fraudEventRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fraud analysis not found for transaction: " + transactionId));

        String recommendation = determineRecommendation(fraudEvent);

        return RiskScoreResponseDto.builder()
                .transactionId(transactionId)
                .riskScore(fraudEvent.getRiskScore())
                .riskLevel(fraudEvent.getRiskLevel().name())
                .isFlagged(fraudEvent.getRiskScore().compareTo(new BigDecimal("25")) > 0)
                .recommendation(recommendation)
                .build();
    }

    @Transactional(readOnly = true)
    public List<FraudEventResponseDto> getAllAlerts() {
        log.info("Fetching all fraud alerts");
        return fraudEventRepository.findAll().stream()
                .map(FraudEventResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudEventResponseDto> getHighRiskAlerts() {
        log.info("Fetching high-risk fraud alerts");
        return fraudEventRepository.findHighRiskPendingReview().stream()
                .map(FraudEventResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudEventResponseDto> getRecentAlerts() {
        log.info("Fetching recent fraud alerts");
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return fraudEventRepository.findRecentEvents(since).stream()
                .map(FraudEventResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudEventResponseDto> getAlertsByAccount(UUID accountId) {
        log.info("Fetching fraud alerts for accountId: {}", accountId);
        return fraudEventRepository.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(FraudEventResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudRuleResponseDto> getAllRules() {
        log.info("Fetching all fraud rules");
        return fraudRuleRepository.findAll().stream()
                .map(FraudRuleResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FraudRuleResponseDto> getEnabledRules() {
        log.info("Fetching enabled fraud rules");
        return fraudRuleRepository.findAllEnabledOrderByPriority().stream()
                .map(FraudRuleResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FraudRuleResponseDto createRule(FraudRuleRequestDto request, UUID createdBy) {
        log.info("Creating fraud rule: {}", request.getRuleName());

        FraudRule rule = FraudRule.builder()
                .ruleName(request.getRuleName())
                .ruleDescription(request.getRuleDescription())
                .description(request.getRuleDescription())
                .ruleType(FraudRule.RuleType.valueOf(request.getRuleType().toUpperCase()))
                .thresholdValue(request.getThresholdValue())
                .thresholdAmount(request.getThresholdValue())
                .timeWindowMinutes(request.getTimeWindowMinutes())
                .riskScorePoints(request.getRiskScorePoints())
                .priority(request.getPriority() != null ? request.getPriority() : 5)
                .enabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .autoBlock(request.getAutoBlock() != null ? request.getAutoBlock() : false)
                .countryBlacklist(request.getCountryBlacklist())
                .build();

        rule = fraudRuleRepository.save(rule);
        log.info("Fraud rule created: {}", rule.getId());
        return FraudRuleResponseDto.fromEntity(rule);
    }

    @Transactional
    public FraudRuleResponseDto updateRule(UUID ruleId, FraudRuleRequestDto request) {
        log.info("Updating fraud rule: {}", ruleId);

        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud rule not found: " + ruleId));

        if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
        if (request.getRuleDescription() != null) {
            rule.setRuleDescription(request.getRuleDescription());
            rule.setDescription(request.getRuleDescription());
        }
        if (request.getThresholdValue() != null) {
            rule.setThresholdValue(request.getThresholdValue());
            rule.setThresholdAmount(request.getThresholdValue());
        }
        if (request.getTimeWindowMinutes() != null) rule.setTimeWindowMinutes(request.getTimeWindowMinutes());
        if (request.getRiskScorePoints() != null) rule.setRiskScorePoints(request.getRiskScorePoints());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getIsEnabled() != null) {
            rule.setEnabled(request.getIsEnabled());
            rule.setIsEnabled(request.getIsEnabled());
        }
        if (request.getAutoBlock() != null) rule.setAutoBlock(request.getAutoBlock());
        if (request.getCountryBlacklist() != null) rule.setCountryBlacklist(request.getCountryBlacklist());

        rule = fraudRuleRepository.save(rule);
        log.info("Fraud rule updated: {}", ruleId);
        return FraudRuleResponseDto.fromEntity(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        log.info("Deleting fraud rule: {}", ruleId);
        fraudRuleRepository.deleteById(ruleId);
        log.info("Fraud rule deleted: {}", ruleId);
    }

    private FraudEvent.RiskLevel calculateRiskLevel(BigDecimal riskScore) {
        int score = riskScore.intValue();
        if (score >= 76) return FraudEvent.RiskLevel.CRITICAL;
        if (score >= 51) return FraudEvent.RiskLevel.HIGH;
        if (score >= 26) return FraudEvent.RiskLevel.MEDIUM;
        return FraudEvent.RiskLevel.LOW;
    }

    private String determineRecommendation(FraudEvent fraudEvent) {
        return switch (fraudEvent.getRiskLevel()) {
            case CRITICAL -> "BLOCK";
            case HIGH -> "REVIEW";
            case MEDIUM -> "MONITOR";
            case LOW -> "ALLOW";
        };
    }

    private void publishEvents(FraudEvent fraudEvent, FraudRulesEngine.FraudEvaluationResult result) {
        if (eventProducer == null) {
            log.debug("Kafka is disabled, skipping event publishing for fraud event: {}", fraudEvent.getId());
            return;
        }

        TransactionFlaggedEvent flaggedEvent = TransactionFlaggedEvent.builder()
                .fraudEventId(fraudEvent.getId())
                .transactionId(fraudEvent.getTransactionId())
                .accountId(fraudEvent.getAccountId())
                .customerId(fraudEvent.getCustomerId())
                .transactionAmount(fraudEvent.getTransactionAmount())
                .transactionType(fraudEvent.getTransactionType())
                .riskScore(fraudEvent.getRiskScore())
                .riskLevel(fraudEvent.getRiskLevel().name())
                .fraudReasons(result.getFraudReasons())
                .rulesTriggered(result.getTriggeredRules().stream()
                        .map(FraudRule::getRuleName)
                        .collect(Collectors.toList()))
                .recommendation(determineRecommendation(fraudEvent))
                .flaggedAt(fraudEvent.getCreatedAt())
                .build();

        eventProducer.publishTransactionFlagged(flaggedEvent);

        if (fraudEvent.isHighRisk()) {
            FraudAlertEvent alertEvent = FraudAlertEvent.builder()
                    .fraudEventId(fraudEvent.getId())
                    .transactionId(fraudEvent.getTransactionId())
                    .accountId(fraudEvent.getAccountId())
                    .customerId(fraudEvent.getCustomerId())
                    .transactionAmount(fraudEvent.getTransactionAmount())
                    .riskScore(fraudEvent.getRiskScore())
                    .riskLevel(fraudEvent.getRiskLevel().name())
                    .fraudReasons(result.getFraudReasons())
                    .actionTaken(fraudEvent.getActionTaken())
                    .alertSeverity(fraudEvent.getRiskLevel().name())
                    .alertedAt(fraudEvent.getCreatedAt())
                    .build();

            eventProducer.publishFraudAlert(alertEvent);
        }
    }
}
