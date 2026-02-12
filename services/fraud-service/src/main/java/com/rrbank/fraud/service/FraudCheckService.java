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

    // Increased limits for realistic banking operations
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("100000.00");        // $100,000 daily limit
    private static final BigDecimal SUSPICIOUS_AMOUNT = new BigDecimal("50000.00");   // Flag transactions over $50k
    private static final BigDecimal PER_TRANSACTION_LIMIT = new BigDecimal("25000.00"); // Max $25k per transaction
    private static final int MAX_WITHDRAWALS_PER_HOUR = 10;

    public FraudCheckResponse checkTransaction(FraudCheckRequest request) {
        log.info("=== FRAUD CHECK START ===");
        log.info("Transaction: type={}, amount={}, accountId={}, userId={}", 
                request.getTransactionType(), request.getAmount(), 
                request.getAccountId(), request.getUserId());

        int riskScore = 0;
        StringBuilder reasons = new StringBuilder();

        // Check per-transaction limit
        if (request.getAmount().compareTo(PER_TRANSACTION_LIMIT) > 0) {
            log.warn("Transaction exceeds per-transaction limit: {} > {}", 
                    request.getAmount(), PER_TRANSACTION_LIMIT);
            return FraudCheckResponse.builder()
                    .decision("REJECT")
                    .reason("Transaction exceeds maximum amount of $" + PER_TRANSACTION_LIMIT + " per transaction")
                    .riskScore(100)
                    .build();
        }

        // Flag suspicious high-value transactions (but don't reject)
        if (request.getAmount().compareTo(SUSPICIOUS_AMOUNT) > 0) {
            riskScore += 30;
            reasons.append("High value transaction (over $").append(SUSPICIOUS_AMOUNT).append("). ");
            log.info("High value transaction flagged: {}", request.getAmount());
        }

        // Check daily limit
        if (request.getUserId() != null) {
            BigDecimal dailyUsed = getDailyUsage(request.getUserId().toString());
            BigDecimal newTotal = dailyUsed.add(request.getAmount());
            
            log.info("Daily usage check: used={}, new={}, limit={}", dailyUsed, newTotal, DAILY_LIMIT);
            
            if (newTotal.compareTo(DAILY_LIMIT) > 0) {
                log.warn("Daily limit would be exceeded for user: {}", request.getUserId());
                return FraudCheckResponse.builder()
                        .decision("REJECT")
                        .reason("Daily transaction limit exceeded. Limit: $" + DAILY_LIMIT + 
                                ", Already used: $" + dailyUsed + 
                                ", Requested: $" + request.getAmount())
                        .riskScore(100)
                        .build();
            }
        }

        // Check withdrawal velocity
        if ("WITHDRAWAL".equals(request.getTransactionType()) && request.getUserId() != null) {
            int withdrawalsThisHour = getWithdrawalsThisHour(request.getUserId().toString());
            log.info("Withdrawal velocity check: count={}, max={}", withdrawalsThisHour, MAX_WITHDRAWALS_PER_HOUR);
            
            if (withdrawalsThisHour >= MAX_WITHDRAWALS_PER_HOUR) {
                log.warn("Velocity limit exceeded for user: {}", request.getUserId());
                return FraudCheckResponse.builder()
                        .decision("REJECT")
                        .reason("Maximum withdrawals per hour exceeded. Max: " + MAX_WITHDRAWALS_PER_HOUR)
                        .riskScore(100)
                        .build();
            }
            riskScore += withdrawalsThisHour * 3; // Lower multiplier
        }

        // Determine decision based on risk score
        String decision;
        if (riskScore >= 100) {
            decision = "REJECT";
        } else if (riskScore >= 70) {
            decision = "REVIEW";
        } else {
            decision = "APPROVE";
            
            // Update tracking only on approval
            if (request.getUserId() != null) {
                updateDailyUsage(request.getUserId().toString(), request.getAmount());
                if ("WITHDRAWAL".equals(request.getTransactionType())) {
                    incrementWithdrawals(request.getUserId().toString());
                }
            }
        }

        String reason = reasons.length() > 0 ? reasons.toString().trim() : "Transaction approved";
        
        log.info("=== FRAUD CHECK RESULT: decision={}, riskScore={}, reason={} ===", 
                decision, riskScore, reason);
        
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
            BigDecimal usage = value != null ? new BigDecimal(value) : BigDecimal.ZERO;
            log.debug("Daily usage for user {}: {}", userId, usage);
            return usage;
        } catch (Exception e) {
            log.warn("Failed to get daily usage from Redis (returning 0): {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void updateDailyUsage(String userId, BigDecimal amount) {
        try {
            String key = "fraud:daily:" + userId;
            BigDecimal current = getDailyUsage(userId);
            BigDecimal newTotal = current.add(amount);
            redisTemplate.opsForValue().set(key, newTotal.toString(), Duration.ofHours(24));
            log.debug("Updated daily usage for user {}: {} -> {}", userId, current, newTotal);
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
