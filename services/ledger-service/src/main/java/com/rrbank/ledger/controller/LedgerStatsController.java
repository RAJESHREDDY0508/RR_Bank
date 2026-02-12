package com.rrbank.ledger.controller;

import com.rrbank.ledger.repository.BalanceCacheRepository;
import com.rrbank.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Slf4j
public class LedgerStatsController {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceCacheRepository balanceCacheRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("GET ledger stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalEntries = ledgerEntryRepository.count();
            stats.put("totalEntries", totalEntries);
            
            // Calculate total balance from balance cache (sum of all account balances)
            BigDecimal totalBalance = balanceCacheRepository.sumAllBalances();
            if (totalBalance == null) {
                totalBalance = BigDecimal.ZERO;
            }
            stats.put("totalBalance", totalBalance);
            
            // Get count of accounts with balance
            long accountsWithBalance = balanceCacheRepository.count();
            stats.put("accountsWithBalance", accountsWithBalance);
            
            log.info("Ledger stats: entries={}, totalBalance={}, accounts={}", 
                    totalEntries, totalBalance, accountsWithBalance);
        } catch (Exception e) {
            log.error("Error getting ledger stats: {}", e.getMessage());
            stats.put("totalEntries", 0L);
            stats.put("totalBalance", BigDecimal.ZERO);
            stats.put("accountsWithBalance", 0L);
        }
        
        return ResponseEntity.ok(stats);
    }
}
