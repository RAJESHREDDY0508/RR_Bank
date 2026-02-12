package com.rrbank.transaction.controller;

import com.rrbank.transaction.entity.Transaction;
import com.rrbank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionStatsController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("GET transaction stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        long totalTransactions = transactionRepository.count();
        long completedTransactions = transactionRepository.countByStatus(Transaction.TransactionStatus.COMPLETED);
        long pendingTransactions = transactionRepository.countByStatus(Transaction.TransactionStatus.PENDING);
        long failedTransactions = transactionRepository.countByStatus(Transaction.TransactionStatus.FAILED);
        
        long transactionsToday = transactionRepository.countCreatedSince(LocalDateTime.now().toLocalDate().atStartOfDay());
        long transactionsThisWeek = transactionRepository.countCreatedSince(LocalDateTime.now().minusDays(7));
        long transactionsThisMonth = transactionRepository.countCreatedSince(LocalDateTime.now().minusDays(30));
        
        BigDecimal volumeToday = transactionRepository.sumAmountSince(LocalDateTime.now().toLocalDate().atStartOfDay());
        BigDecimal volumeThisWeek = transactionRepository.sumAmountSince(LocalDateTime.now().minusDays(7));
        BigDecimal volumeThisMonth = transactionRepository.sumAmountSince(LocalDateTime.now().minusDays(30));
        
        stats.put("totalTransactions", totalTransactions);
        stats.put("completedTransactions", completedTransactions);
        stats.put("pendingTransactions", pendingTransactions);
        stats.put("failedTransactions", failedTransactions);
        stats.put("transactionsToday", transactionsToday);
        stats.put("transactionsThisWeek", transactionsThisWeek);
        stats.put("transactionsThisMonth", transactionsThisMonth);
        stats.put("volumeToday", volumeToday);
        stats.put("volumeThisWeek", volumeThisWeek);
        stats.put("volumeThisMonth", volumeThisMonth);
        
        // By type breakdown
        Map<String, Long> byType = new HashMap<>();
        byType.put("DEPOSIT", transactionRepository.countByTransactionType(Transaction.TransactionType.DEPOSIT));
        byType.put("WITHDRAWAL", transactionRepository.countByTransactionType(Transaction.TransactionType.WITHDRAWAL));
        byType.put("TRANSFER", transactionRepository.countByTransactionType(Transaction.TransactionType.TRANSFER));
        byType.put("PAYMENT", transactionRepository.countByTransactionType(Transaction.TransactionType.PAYMENT));
        stats.put("byType", byType);
        
        return ResponseEntity.ok(stats);
    }
}
