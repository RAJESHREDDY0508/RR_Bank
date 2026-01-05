package com.rrbank.ledger.service;

import com.rrbank.ledger.dto.LedgerDTOs.*;
import com.rrbank.ledger.entity.BalanceCache;
import com.rrbank.ledger.entity.LedgerEntry;
import com.rrbank.ledger.event.LedgerEventProducer;
import com.rrbank.ledger.repository.BalanceCacheRepository;
import com.rrbank.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final BalanceCacheRepository balanceCacheRepository;
    private final LedgerEventProducer eventProducer;

    /**
     * Credit an account (increase balance)
     */
    @Transactional
    public LedgerEntryResponse credit(CreateEntryRequest request) {
        log.info("Crediting {} to account {}", request.getAmount(), request.getAccountId());
        
        LedgerEntry entry = createEntry(request.getAccountId(), request.getTransactionId(),
                LedgerEntry.EntryType.CREDIT, request.getAmount(), request.getDescription());
        
        eventProducer.publishLedgerEntryCreated(entry);
        return toResponse(entry);
    }

    /**
     * Debit an account (decrease balance)
     * Throws exception if insufficient balance
     */
    @Transactional
    public LedgerEntryResponse debit(CreateEntryRequest request) {
        log.info("Debiting {} from account {}", request.getAmount(), request.getAccountId());
        
        // Check balance first
        BigDecimal currentBalance = getBalanceInternal(request.getAccountId());
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance. Current: " + currentBalance + ", Required: " + request.getAmount());
        }
        
        LedgerEntry entry = createEntry(request.getAccountId(), request.getTransactionId(),
                LedgerEntry.EntryType.DEBIT, request.getAmount(), request.getDescription());
        
        eventProducer.publishLedgerEntryCreated(entry);
        return toResponse(entry);
    }

    /**
     * Transfer between accounts (atomic debit + credit)
     */
    @Transactional
    public void transfer(TransferRequest request) {
        log.info("Transferring {} from {} to {}", 
                request.getAmount(), request.getFromAccountId(), request.getToAccountId());
        
        // Debit source
        CreateEntryRequest debitReq = CreateEntryRequest.builder()
                .accountId(request.getFromAccountId())
                .transactionId(request.getTransactionId())
                .entryType("DEBIT")
                .amount(request.getAmount())
                .description("Transfer to " + request.getToAccountId())
                .build();
        debit(debitReq);
        
        // Credit destination
        CreateEntryRequest creditReq = CreateEntryRequest.builder()
                .accountId(request.getToAccountId())
                .transactionId(request.getTransactionId())
                .entryType("CREDIT")
                .amount(request.getAmount())
                .description("Transfer from " + request.getFromAccountId())
                .build();
        credit(creditReq);
    }

    /**
     * Get account balance - SOURCE OF TRUTH calculation
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        BigDecimal balance = getBalanceInternal(accountId);
        return BalanceResponse.builder()
                .accountId(accountId.toString())
                .balance(balance)
                .asOf(LocalDateTime.now())
                .build();
    }

    /**
     * Get ledger entries for an account
     */
    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getEntries(UUID accountId, Pageable pageable) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(this::toResponse);
    }

    /**
     * Rebuild balance cache from ledger entries
     * Can be used for reconciliation
     */
    @Transactional
    public void rebuildBalanceCache(UUID accountId) {
        log.info("Rebuilding balance cache for account: {}", accountId);
        BigDecimal balance = ledgerEntryRepository.calculateBalance(accountId);
        
        BalanceCache cache = balanceCacheRepository.findById(accountId)
                .orElse(BalanceCache.builder().accountId(accountId).build());
        cache.setBalance(balance);
        cache.setLastUpdated(LocalDateTime.now());
        balanceCacheRepository.save(cache);
    }

    private LedgerEntry createEntry(UUID accountId, UUID transactionId, 
                                     LedgerEntry.EntryType entryType, 
                                     BigDecimal amount, String description) {
        // Calculate new running balance
        BigDecimal currentBalance = getBalanceInternal(accountId);
        BigDecimal newBalance = entryType == LedgerEntry.EntryType.CREDIT 
                ? currentBalance.add(amount) 
                : currentBalance.subtract(amount);

        LedgerEntry entry = LedgerEntry.builder()
                .accountId(accountId)
                .transactionId(transactionId)
                .entryType(entryType)
                .amount(amount)
                .runningBalance(newBalance)
                .description(description)
                .build();
        
        entry = ledgerEntryRepository.save(entry);
        
        // Update cache
        updateBalanceCache(accountId, newBalance);
        
        eventProducer.publishBalanceUpdated(accountId, newBalance);
        
        return entry;
    }

    private BigDecimal getBalanceInternal(UUID accountId) {
        BigDecimal balance = ledgerEntryRepository.calculateBalance(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    private void updateBalanceCache(UUID accountId, BigDecimal balance) {
        BalanceCache cache = balanceCacheRepository.findById(accountId)
                .orElse(BalanceCache.builder().accountId(accountId).build());
        cache.setBalance(balance);
        cache.setLastUpdated(LocalDateTime.now());
        balanceCacheRepository.save(cache);
    }

    private LedgerEntryResponse toResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId().toString())
                .accountId(entry.getAccountId().toString())
                .transactionId(entry.getTransactionId() != null ? entry.getTransactionId().toString() : null)
                .entryType(entry.getEntryType().name())
                .amount(entry.getAmount())
                .runningBalance(entry.getRunningBalance())
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
