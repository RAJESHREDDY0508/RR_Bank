package com.rrbank.fraud.service;

import com.rrbank.fraud.dto.FraudDTOs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCheckService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("10000.00");
    private static final BigDecimal SUSPICIOUS_AMOUNT = new BigDecimal("5000.00");
    private static final int MAX_WITHDRAWALS_PER_HOUR = 5;

    public FraudCheckResponse checkTransaction(FraudCheckRequest request) {
        log.info("Checking transaction: type={}, amount={}", request.getTransactionType(), request.getAmount());

        int riskScore = 0;
        StringBuilder reasons = new StringBuilder();

        if (request.getAmount().compareTo(SUSPICIOUS_AMOUNT) > 0) {
            riskScore += 30;
            reasons.append("High value transaction. ");
        }

        if (request.getUserId() != null) {
            BigDecimal dailyUsed = getDailyUsage(request.getUserId().toString());
            BigDecimal newTotal = dailyUsed.add(request.getAmount());
            
            if (newTotal.compareTo(DAILY_LIMIT) > 0) {
                log.warn("Daily limit exceeded for user: {}", request.getUserId());
                return FraudCheckResponse.builder()
                        .decision("REJECT")
                        .reason("Daily transaction limit exceeded. Limit: " + DAILY_LIMIT + ", Used: " + dailyUsed)
                        .riskScore(100)
                        .build();
            }
        }

        if ("WITHDRAWAL".equals(request.getTransactionType()) && request.getUserId() != null) {
            int withdrawalsThisHour = getWithdrawalsThisHour(request.getUserId().toString());
            if (withdrawalsThisHour >= MAX_WITHDRAWALS_PER_HOUR) {
                log.warn("Velocity limit exceeded for user: {}", request.getUserId());
                return FraudCheckResponse.builder()
                        .decision("REJECT")
                        .reason("Maximum withdrawals per hour exceeded. Max: " + MAX_WITHDRAWALS_PER_HOUR)
                        .riskScore(100)
                        .build();
            }
            riskScore += withdrawalsThisHour * 5;
        }

        String decision;
        if (riskScore >= 70) {
            decision = "REVIEW";
        } else if (riskScore >= 100) {
            decision = "REJECT";
        } else {
            decision = "APPROVE";
            if (request.getUserId() != null) {
                updateDailyUsage(request.getUserId().toString(), request.getAmount());
                if ("WITHDRAWAL".equals(request.getTransactionType())) {
                    incrementWithdrawals(request.getUserId().toString());
                }
            }
        }

        String reason = reasons.length() > 0 ? reasons.toString().trim() : "Transaction approved";
        
        log.info("Fraud check result: decision={}, riskScore={}", decision, riskScore);
        return FraudCheckResponse.builder()
                .decision(decision)
                .reason(reason)
                .riskScore(riskScore)
                .build();
    }

    public UserLimitsResponse getUserLimits(String userId) {
        BigDecimal dailyUsed = getDailyUsage(userId);
        int withdrawals = getWithdrawalsThisHour(userId);

        return UserLimitsResponse.builder()
                .userId(userId)
                .dailyLimit(DAILY_LIMIT)
                .dailyUsed(dailyUsed)
                .remainingDaily(DAILY_LIMIT.subtract(dailyUsed))
                .maxWithdrawalsPerHour(MAX_WITHDRAWALS_PER_HOUR)
                .withdrawalsThisHour(withdrawals)
                .build();
    }

    private BigDecimal getDailyUsage(String userId) {
        try {
            String key = "fraud:daily:" + userId;
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? new BigDecimal(value) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get daily usage from Redis: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void updateDailyUsage(String userId, BigDecimal amount) {
        try {
            String key = "fraud:daily:" + userId;
            BigDecimal current = getDailyUsage(userId);
            BigDecimal newTotal = current.add(amount);
            redisTemplate.opsForValue().set(key, newTotal.toString(), Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("Failed to update daily usage in Redis: {}", e.getMessage());
        }
    }

    private int getWithdrawalsThisHour(String userId) {
        try {
            String key = "fraud:velocity:" + userId;
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value) : 0;
        } catch (Exception e) {
            log.warn("Failed to get withdrawal count from Redis: {}", e.getMessage());
            return 0;
        }
    }

    private void incrementWithdrawals(String userId) {
        try {
            String key = "fraud:velocity:" + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to increment withdrawal count in Redis: {}", e.getMessage());
        }
    }
}
