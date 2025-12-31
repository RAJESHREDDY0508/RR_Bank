package com.RRBank.banking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account Cache Service
 * Simple in-memory cache - no Redis dependency
 */
@Service
@Slf4j
public class AccountCacheService {

    private final Map<String, BigDecimal> balanceCache = new ConcurrentHashMap<>();
    private final Map<String, Object> detailsCache = new ConcurrentHashMap<>();

    public AccountCacheService() {
        log.info("AccountCacheService initialized with in-memory cache");
    }

    public BigDecimal getCachedBalance(UUID accountId) {
        return balanceCache.get(accountId.toString());
    }

    public void cacheBalance(UUID accountId, BigDecimal balance) {
        balanceCache.put(accountId.toString(), balance);
    }

    public void invalidateBalance(UUID accountId) {
        balanceCache.remove(accountId.toString());
    }

    public void updateCachedBalance(UUID accountId, BigDecimal newBalance) {
        balanceCache.put(accountId.toString(), newBalance);
    }

    public void cacheAccountDetails(UUID accountId, Object accountDetails) {
        detailsCache.put(accountId.toString(), accountDetails);
    }

    public Object getCachedAccountDetails(UUID accountId) {
        return detailsCache.get(accountId.toString());
    }

    public void invalidateAllAccountCache(UUID accountId) {
        balanceCache.remove(accountId.toString());
        detailsCache.remove(accountId.toString());
    }
}
