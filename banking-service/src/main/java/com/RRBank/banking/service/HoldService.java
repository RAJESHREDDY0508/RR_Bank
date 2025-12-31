package com.RRBank.banking.service;

import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Hold;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.HoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Hold Service
 * Phase 3: Manages holds on account balances
 * 
 * Available balance = balance - active holds
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HoldService {

    private final HoldRepository holdRepository;
    private final AccountRepository accountRepository;

    /**
     * Create a new hold on an account
     */
    @Transactional
    public Hold createHold(UUID accountId, BigDecimal amount, Hold.HoldType holdType,
                          String reason, LocalDateTime expiresAt, UUID createdBy) {
        log.info("Creating hold on account {} for amount {} type {}", accountId, amount, holdType);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        Hold hold = Hold.builder()
                .accountId(accountId)
                .amount(amount)
                .currency(account.getCurrency())
                .holdType(holdType)
                .reason(reason)
                .expiresAt(expiresAt)
                .createdBy(createdBy)
                .reference("HOLD-" + System.currentTimeMillis())
                .build();

        hold = holdRepository.save(hold);
        log.info("Hold created: {}", hold.getId());

        return hold;
    }

    /**
     * Create a hold linked to a transaction
     */
    @Transactional
    public Hold createTransactionHold(UUID accountId, UUID transactionId, BigDecimal amount,
                                      Hold.HoldType holdType, String reason, 
                                      LocalDateTime expiresAt, UUID createdBy) {
        Hold hold = createHold(accountId, amount, holdType, reason, expiresAt, createdBy);
        hold.setTransactionId(transactionId);
        return holdRepository.save(hold);
    }

    /**
     * Release a hold
     */
    @Transactional
    public Hold releaseHold(UUID holdId, UUID releasedBy, String reason) {
        log.info("Releasing hold: {}", holdId);

        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));

        if (!hold.isActive()) {
            throw new IllegalStateException("Hold is not active");
        }

        hold.release(releasedBy, reason);
        hold = holdRepository.save(hold);

        log.info("Hold released: {}", holdId);
        return hold;
    }

    /**
     * Capture a hold (convert to actual debit)
     */
    @Transactional
    public Hold captureHold(UUID holdId) {
        log.info("Capturing hold: {}", holdId);

        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));

        if (!hold.isActive()) {
            throw new IllegalStateException("Hold is not active");
        }

        hold.capture();
        hold = holdRepository.save(hold);

        log.info("Hold captured: {}", holdId);
        return hold;
    }

    /**
     * Get available balance for an account
     * Available = Balance - Active Holds
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        BigDecimal totalHolds = holdRepository.getTotalActiveHoldsAmount(accountId);
        if (totalHolds == null) totalHolds = BigDecimal.ZERO;

        BigDecimal available = account.getBalance().subtract(totalHolds);
        log.debug("Account {} balance: {}, holds: {}, available: {}", 
                accountId, account.getBalance(), totalHolds, available);

        return available.max(BigDecimal.ZERO);
    }

    /**
     * Get all active holds for an account
     */
    @Transactional(readOnly = true)
    public List<Hold> getActiveHolds(UUID accountId) {
        return holdRepository.findActiveHoldsByAccount(accountId);
    }

    /**
     * Get hold by ID
     */
    @Transactional(readOnly = true)
    public Hold getHoldById(UUID holdId) {
        return holdRepository.findById(holdId)
                .orElseThrow(() -> new ResourceNotFoundException("Hold not found: " + holdId));
    }

    /**
     * Scheduled job to expire holds
     * Runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireHolds() {
        log.info("Running scheduled hold expiration");
        int expired = holdRepository.expireHolds(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} holds", expired);
        }
    }

    /**
     * Check if account has sufficient available balance
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientAvailableBalance(UUID accountId, BigDecimal amount) {
        BigDecimal available = getAvailableBalance(accountId);
        return available.compareTo(amount) >= 0;
    }
}
