package com.RRBank.banking.service;

import com.RRBank.banking.dto.TransactionLimitResponse;
import com.RRBank.banking.entity.TransactionLimit;
import com.RRBank.banking.entity.VelocityCheck;
import com.RRBank.banking.exception.BusinessException;
import com.RRBank.banking.repository.TransactionLimitRepository;
import com.RRBank.banking.repository.VelocityCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Transaction Limit Service - Enforce transaction limits and velocity checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionLimitService {

    private final TransactionLimitRepository limitRepository;
    private final VelocityCheckRepository velocityRepository;

    @Transactional
    public void validateTransaction(String userId, BigDecimal amount, TransactionLimit.LimitType transactionType) {
        log.info("Validating limits for user {} amount {} type {}", userId, amount, transactionType);
        
        checkVelocity(userId);
        
        List<TransactionLimit> limits = limitRepository.findApplicableLimits(userId, transactionType);
        
        if (limits.isEmpty()) {
            createDefaultLimit(userId);
            limits = limitRepository.findApplicableLimits(userId, transactionType);
        }
        
        for (TransactionLimit limit : limits) {
            if (limit.needsDailyReset()) limit.resetDaily();
            if (limit.needsMonthlyReset()) limit.resetMonthly();
            
            if (!limit.isWithinPerTransactionLimit(amount)) {
                throw new BusinessException(String.format(
                    "Transaction amount $%s exceeds per-transaction limit of $%s",
                    amount, limit.getPerTransactionLimit()));
            }
            
            if (!limit.isWithinDailyLimit(amount)) {
                throw new BusinessException(String.format(
                    "Transaction would exceed daily limit. Remaining: $%s, Requested: $%s",
                    limit.getRemainingDaily(), amount));
            }
            
            if (!limit.isWithinMonthlyLimit(amount)) {
                throw new BusinessException(String.format(
                    "Transaction would exceed monthly limit. Remaining: $%s, Requested: $%s",
                    limit.getRemainingMonthly(), amount));
            }
        }
    }

    @Transactional
    public void consumeLimits(String userId, BigDecimal amount, TransactionLimit.LimitType transactionType) {
        List<TransactionLimit> limits = limitRepository.findApplicableLimits(userId, transactionType);
        for (TransactionLimit limit : limits) {
            limit.consumeLimit(amount);
            limitRepository.save(limit);
        }
        updateVelocity(userId);
    }

    @Transactional
    public void checkVelocity(String userId) {
        if (velocityRepository.isUserBlocked(userId)) {
            List<VelocityCheck> blocked = velocityRepository.findBlockedByUserId(userId);
            if (!blocked.isEmpty()) {
                throw new BusinessException("Too many transactions. Try again after " + blocked.get(0).getBlockedUntil());
            }
        }
        
        VelocityCheck velocity = velocityRepository
            .findByUserIdAndCheckType(userId, VelocityCheck.CheckType.TRANSACTION_COUNT)
            .orElseGet(() -> createDefaultVelocityCheck(userId));
        
        if (velocity.isWindowExpired()) velocity.resetWindow();
        
        if (velocity.isLimitExceeded()) {
            velocity.block(30);
            velocityRepository.save(velocity);
            throw new BusinessException("Too many transactions in short time. Please wait 30 minutes.");
        }
    }

    @Transactional
    public void updateVelocity(String userId) {
        VelocityCheck velocity = velocityRepository
            .findByUserIdAndCheckType(userId, VelocityCheck.CheckType.TRANSACTION_COUNT)
            .orElseGet(() -> createDefaultVelocityCheck(userId));
        velocity.increment();
        velocityRepository.save(velocity);
    }

    @Transactional(readOnly = true)
    public List<TransactionLimitResponse> getLimitsForUser(String userId) {
        List<TransactionLimit> limits = limitRepository.findByUserIdAndEnabledTrue(userId);
        for (TransactionLimit limit : limits) {
            if (limit.needsDailyReset() || limit.needsMonthlyReset()) {
                if (limit.needsDailyReset()) limit.resetDaily();
                if (limit.needsMonthlyReset()) limit.resetMonthly();
                limitRepository.save(limit);
            }
        }
        return limits.stream().map(TransactionLimitResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public TransactionLimitResponse updateLimit(String userId, TransactionLimit.LimitType limitType,
                                               BigDecimal dailyLimit, BigDecimal perTransactionLimit,
                                               BigDecimal monthlyLimit) {
        TransactionLimit limit = limitRepository.findByUserIdAndLimitType(userId, limitType)
            .orElseGet(() -> TransactionLimit.builder().userId(userId).limitType(limitType).build());
        
        if (dailyLimit != null) limit.setDailyLimit(dailyLimit);
        if (perTransactionLimit != null) limit.setPerTransactionLimit(perTransactionLimit);
        if (monthlyLimit != null) limit.setMonthlyLimit(monthlyLimit);
        
        return TransactionLimitResponse.fromEntity(limitRepository.save(limit));
    }

    @Transactional
    public TransactionLimit createDefaultLimit(String userId) {
        return limitRepository.save(TransactionLimit.builder()
            .userId(userId)
            .limitType(TransactionLimit.LimitType.ALL)
            .dailyLimit(new BigDecimal("10000.00"))
            .perTransactionLimit(new BigDecimal("5000.00"))
            .monthlyLimit(new BigDecimal("100000.00"))
            .enabled(true)
            .build());
    }

    private VelocityCheck createDefaultVelocityCheck(String userId) {
        return velocityRepository.save(VelocityCheck.builder()
            .userId(userId)
            .checkType(VelocityCheck.CheckType.TRANSACTION_COUNT)
            .windowMinutes(60)
            .maxCount(20)
            .currentCount(0)
            .build());
    }
}
