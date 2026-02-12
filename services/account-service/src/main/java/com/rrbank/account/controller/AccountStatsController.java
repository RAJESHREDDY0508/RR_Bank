package com.rrbank.account.controller;

import com.rrbank.account.entity.Account;
import com.rrbank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountStatsController {

    private final AccountRepository accountRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("GET account stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        long totalAccounts = accountRepository.count();
        long activeAccounts = accountRepository.countByStatusEnum(Account.AccountStatus.ACTIVE);
        long frozenAccounts = accountRepository.countByStatusEnum(Account.AccountStatus.FROZEN);
        long closedAccounts = accountRepository.countByStatusEnum(Account.AccountStatus.CLOSED);
        long pendingAccounts = accountRepository.countByStatusEnum(Account.AccountStatus.PENDING);
        
        stats.put("totalAccounts", totalAccounts);
        stats.put("activeAccounts", activeAccounts);
        stats.put("frozenAccounts", frozenAccounts);
        stats.put("closedAccounts", closedAccounts);
        stats.put("pendingAccounts", pendingAccounts);
        
        // Account types breakdown
        Map<String, Long> byType = new HashMap<>();
        byType.put("CHECKING", accountRepository.countByAccountTypeEnum(Account.AccountType.CHECKING));
        byType.put("SAVINGS", accountRepository.countByAccountTypeEnum(Account.AccountType.SAVINGS));
        byType.put("BUSINESS", accountRepository.countByAccountTypeEnum(Account.AccountType.BUSINESS));
        byType.put("CREDIT", accountRepository.countByAccountTypeEnum(Account.AccountType.CREDIT));
        stats.put("byType", byType);
        
        return ResponseEntity.ok(stats);
    }
}
