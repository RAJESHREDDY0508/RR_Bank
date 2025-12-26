package com.RRBank.banking.service;

import com.RRBank.banking.dto.LedgerEntryResponse;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.LedgerEntry;
import com.RRBank.banking.exception.InsufficientFundsException;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ledger Service - Core Banking Ledger System
 * 
 * Implements double-entry bookkeeping for all financial transactions.
 * Balance = SUM(CREDITS) - SUM(DEBITS)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public LedgerEntry createCreditEntry(UUID accountId, BigDecimal amount, 
                                         UUID transactionId, String referenceId, 
                                         String description) {
        log.info("Creating CREDIT entry for account {} amount {}", accountId, amount);
        
        validateAmount(amount);
        
        BigDecimal currentBalance = calculateBalance(accountId);
        BigDecimal newBalance = currentBalance.add(amount);
        
        LedgerEntry entry = LedgerEntry.builder()
            .accountId(accountId)
            .transactionId(transactionId)
            .entryType(LedgerEntry.EntryType.CREDIT)
            .amount(amount)
            .currency("USD")
            .runningBalance(newBalance)
            .referenceId(referenceId)
            .description(description)
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        updateAccountBalance(accountId, newBalance);
        
        log.info("CREDIT entry created: {} | Balance: {} -> {}", 
                entry.getId(), currentBalance, newBalance);
        
        return entry;
    }

    @Transactional
    public LedgerEntry createDebitEntry(UUID accountId, BigDecimal amount, 
                                        UUID transactionId, String referenceId, 
                                        String description) {
        log.info("Creating DEBIT entry for account {} amount {}", accountId, amount);
        
        validateAmount(amount);
        
        BigDecimal currentBalance = calculateBalance(accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        
        BigDecimal availableBalance = currentBalance.add(
            account.getOverdraftLimit() != null ? account.getOverdraftLimit() : BigDecimal.ZERO
        );
        
        if (amount.compareTo(availableBalance) > 0) {
            throw new InsufficientFundsException(
                "Insufficient funds. Available: " + availableBalance + ", Requested: " + amount
            );
        }
        
        BigDecimal newBalance = currentBalance.subtract(amount);
        
        LedgerEntry entry = LedgerEntry.builder()
            .accountId(accountId)
            .transactionId(transactionId)
            .entryType(LedgerEntry.EntryType.DEBIT)
            .amount(amount)
            .currency("USD")
            .runningBalance(newBalance)
            .referenceId(referenceId)
            .description(description)
            .build();
        
        entry = ledgerEntryRepository.save(entry);
        updateAccountBalance(accountId, newBalance);
        
        log.info("DEBIT entry created: {} | Balance: {} -> {}", 
                entry.getId(), currentBalance, newBalance);
        
        return entry;
    }

    @Transactional
    public void executeTransfer(UUID fromAccountId, UUID toAccountId, 
                               BigDecimal amount, UUID transactionId, 
                               String description) {
        log.info("Executing transfer from {} to {} amount {}", 
                fromAccountId, toAccountId, amount);
        
        validateAmount(amount);
        
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to same account");
        }
        
        String referenceId = "TXN-" + transactionId.toString().substring(0, 8);
        
        createDebitEntry(fromAccountId, amount, transactionId, 
                        referenceId, "Transfer out: " + description);
        
        createCreditEntry(toAccountId, amount, transactionId, 
                         referenceId, "Transfer in: " + description);
        
        log.info("Transfer completed: {} -> {} amount {}", 
                fromAccountId, toAccountId, amount);
    }

    @Transactional
    public LedgerEntry executeDeposit(UUID accountId, BigDecimal amount, 
                                     UUID transactionId, String description) {
        String referenceId = transactionId != null ? 
            "DEP-" + transactionId.toString().substring(0, 8) : "DEP-INIT";
        return createCreditEntry(accountId, amount, transactionId, 
                                referenceId, "Deposit: " + description);
    }

    @Transactional
    public LedgerEntry executeWithdrawal(UUID accountId, BigDecimal amount, 
                                        UUID transactionId, String description) {
        String referenceId = "WTH-" + transactionId.toString().substring(0, 8);
        return createDebitEntry(accountId, amount, transactionId, 
                               referenceId, "Withdrawal: " + description);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateBalance(UUID accountId) {
        BigDecimal balance = ledgerEntryRepository.calculateBalance(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateBalanceAsOf(UUID accountId, LocalDateTime asOfDate) {
        BigDecimal balance = ledgerEntryRepository.calculateBalanceAsOf(accountId, asOfDate);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getEntriesForAccount(UUID accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
            .stream()
            .map(LedgerEntryResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryResponse> getEntriesForAccount(UUID accountId, Pageable pageable) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
            .map(LedgerEntryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalCredits(UUID accountId) {
        return ledgerEntryRepository.getTotalCredits(accountId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalDebits(UUID accountId) {
        return ledgerEntryRepository.getTotalDebits(accountId);
    }

    @Transactional(readOnly = true)
    public boolean reconcileBalance(UUID accountId) {
        BigDecimal ledgerBalance = calculateBalance(accountId);
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        
        boolean matches = ledgerBalance.compareTo(account.getBalance()) == 0;
        
        if (!matches) {
            log.warn("Balance mismatch for account {}! Ledger: {}, Account: {}", 
                    accountId, ledgerBalance, account.getBalance());
        }
        
        return matches;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    
    private void updateAccountBalance(UUID accountId, BigDecimal newBalance) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        
        account.setBalance(newBalance);
        account.setAvailableBalance(newBalance.add(
            account.getOverdraftLimit() != null ? account.getOverdraftLimit() : BigDecimal.ZERO
        ));
        account.setLastTransactionDate(LocalDateTime.now());
        
        accountRepository.save(account);
    }
}
