package com.rrbank.fraud.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudStatsController {

    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("GET fraud stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Count pending alerts (in a real system, this would be from a database)
        // For now, we'll count users with high velocity
        long pendingAlerts = 0;
        long reviewRequired = 0;
        long blockedTransactions = 0;
        
        try {
            // Count users with velocity tracking (potential fraud monitoring)
            Set<String> velocityKeys = redisTemplate.keys("fraud:velocity:*");
            if (velocityKeys != null) {
                pendingAlerts = velocityKeys.size();
            }
            
            Set<String> dailyKeys = redisTemplate.keys("fraud:daily:*");
            if (dailyKeys != null) {
                reviewRequired = dailyKeys.size();
            }
        } catch (Exception e) {
            log.warn("Failed to get fraud stats from Redis: {}", e.getMessage());
        }
        
        stats.put("pendingAlerts", pendingAlerts);
        stats.put("reviewRequired", reviewRequired);
        stats.put("blockedTransactions", blockedTransactions);
        stats.put("alertsToday", 0L);
        stats.put("alertsThisWeek", 0L);
        stats.put("alertsThisMonth", 0L);
        
        return ResponseEntity.ok(stats);
    }
}
