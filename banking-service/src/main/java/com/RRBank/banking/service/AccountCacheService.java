package com.RRBank.banking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

/**
 * Account Cache Service
 * Redis caching for account balances
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BALANCE_CACHE_PREFIX = "account:";
    private static final String BALANCE_CACHE_SUFFIX = ":balance";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    /**
     * Get cached balance for account
     */
    public BigDecimal getCachedBalance(UUID accountId) {
        try {
            String key = buildBalanceKey(accountId);
            Object cachedBalance = redisTemplate.opsForValue().get(key);
            
            if (cachedBalance != null) {
                log.debug("Cache HIT for account balance: {}", accountId);
                return new BigDecimal(cachedBalance.toString());
            }
            
            log.debug("Cache MISS for account balance: {}", accountId);
            return null;
        } catch (Exception e) {
            log.error("Error getting cached balance for accountId: {}", accountId, e);
            return null;
        }
    }

    /**
     * Cache account balance
     */
    public void cacheBalance(UUID accountId, BigDecimal balance) {
        try {
            String key = buildBalanceKey(accountId);
            redisTemplate.opsForValue().set(key, balance.toString(), CACHE_TTL);
            log.debug("Cached balance for account: {}, balance: {}", accountId, balance);
        } catch (Exception e) {
            log.error("Error caching balance for accountId: {}", accountId, e);
        }
    }

    /**
     * Invalidate (delete) cached balance
     */
    public void invalidateBalance(UUID accountId) {
        try {
            String key = buildBalanceKey(accountId);
            redisTemplate.delete(key);
            log.debug("Invalidated cache for account: {}", accountId);
        } catch (Exception e) {
            log.error("Error invalidating cache for accountId: {}", accountId, e);
        }
    }

    /**
     * Update cached balance (invalidate then cache new value)
     */
    public void updateCachedBalance(UUID accountId, BigDecimal newBalance) {
        invalidateBalance(accountId);
        cacheBalance(accountId, newBalance);
    }

    /**
     * Cache account details (full object)
     */
    public void cacheAccountDetails(UUID accountId, Object accountDetails) {
        try {
            String key = "account:" + accountId + ":details";
            redisTemplate.opsForValue().set(key, accountDetails, CACHE_TTL);
            log.debug("Cached account details for: {}", accountId);
        } catch (Exception e) {
            log.error("Error caching account details for accountId: {}", accountId, e);
        }
    }

    /**
     * Get cached account details
     */
    public Object getCachedAccountDetails(UUID accountId) {
        try {
            String key = "account:" + accountId + ":details";
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error getting cached account details for accountId: {}", accountId, e);
            return null;
        }
    }

    /**
     * Invalidate all cache for account
     */
    public void invalidateAllAccountCache(UUID accountId) {
        try {
            invalidateBalance(accountId);
            String detailsKey = "account:" + accountId + ":details";
            redisTemplate.delete(detailsKey);
            log.debug("Invalidated all cache for account: {}", accountId);
        } catch (Exception e) {
            log.error("Error invalidating all cache for accountId: {}", accountId, e);
        }
    }

    /**
     * Build cache key for balance
     */
    private String buildBalanceKey(UUID accountId) {
        return BALANCE_CACHE_PREFIX + accountId + BALANCE_CACHE_SUFFIX;
    }
}
