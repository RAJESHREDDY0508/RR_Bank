package com.RRBank.banking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Simple In-Memory Cache Configuration
 * Uses ConcurrentHashMap - no Redis dependency
 */
@Configuration
@Slf4j
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        log.info("Using in-memory cache (ConcurrentMapCacheManager)");
        return new ConcurrentMapCacheManager("accounts", "balances", "transactions");
    }
}
