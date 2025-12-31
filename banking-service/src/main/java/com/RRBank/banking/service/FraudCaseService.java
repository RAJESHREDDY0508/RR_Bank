package com.RRBank.banking.service;

import com.RRBank.banking.entity.*;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fraud Case Service
 * Phase 3: Admin fraud review workflow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepository fraudCaseRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final HoldService holdService;

    /**
     * Create a fraud case for a transaction
     */
    @Transactional
    public FraudCase createCase(UUID accountId, UUID transactionId, FraudCase.CaseType caseType,
                                String description, Integer riskScore, String detectionMethod) {
        log.info("Creating fraud case for account {} transaction {}", accountId, transactionId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        Transaction transaction = null;
        if (transactionId != null) {
            transaction = transactionRepository.findById(transactionId).orElse(null);
        }

        FraudCase fraudCase = FraudCase.builder()
                .accountId(accountId)
                .customerId(account.getCustomerId())
                .transactionId(transactionId)
                .amount(transaction != null ? transaction.getAmount() : null)
                .caseType(caseType)
                .description(description)
                .riskScore(riskScore)
                .detectionMethod(detectionMethod)
                .priority(determinePriority(riskScore))
                .dueDate(LocalDateTime.now().plusDays(7))
                .build();

        fraudCase = fraudCaseRepository.save(fraudCase);
        log.info("Fraud case created: {}", fraudCase.getCaseNumber());

        // Create hold on transaction amount if applicable
        if (transaction != null && transaction.getAmount() != null) {
            try {
                Hold hold = holdService.createTransactionHold(
                        accountId, transactionId, transaction.getAmount(),
                        Hold.HoldType.FRAUD_REVIEW,
                        "Fraud case: " + fraudCase.getCaseNumber(),
                        LocalDateTime.now().plusDays(30),
                        null
                );
                fraudCase.setHoldId(hold.getId());
                fraudCase = fraudCaseRepository.save(fraudCase);
            } catch (Exception e) {
                log.warn("Could not create hold for fraud case: {}", e.getMessage());
            }
        }

        return fraudCase;
    }

    private FraudCase.Priority determinePriority(Integer riskScore) {
        if (riskScore == null) return FraudCase.Priority.MEDIUM;
        if (riskScore >= 90) return FraudCase.Priority.CRITICAL;
        if (riskScore >= 70) return FraudCase.Priority.HIGH;
        if (riskScore >= 50) return FraudCase.Priority.MEDIUM;
        return FraudCase.Priority.LOW;
    }

    /**
     * Get open fraud cases (admin queue)
     */
    @Transactional(readOnly = true)
    public Page<FraudCase> getOpenCases(Pageable pageable) {
        return fraudCaseRepository.findOpenCases(pageable);
    }

    /**
     * Get fraud case by ID
     */
    @Transactional(readOnly = true)
    public FraudCase getCaseById(UUID caseId) {
        return fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud case not found: " + caseId));
    }

    /**
     * Get case by case number
     */
    @Transactional(readOnly = true)
    public FraudCase getCaseByCaseNumber(String caseNumber) {
        return fraudCaseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud case not found: " + caseNumber));
    }

    /**
     * Assign case to admin
     */
    @Transactional
    public FraudCase assignCase(UUID caseId, UUID assigneeId) {
        log.info("Assigning fraud case {} to {}", caseId, assigneeId);

        FraudCase fraudCase = getCaseById(caseId);
        fraudCase.assignTo(assigneeId);

        return fraudCaseRepository.save(fraudCase);
    }

    /**
     * Approve transaction (clear fraud case)
     */
    @Transactional
    public FraudCase approveTransaction(UUID caseId, UUID reviewerId, String notes) {
        log.info("Approving fraud case {}", caseId);

        FraudCase fraudCase = getCaseById(caseId);
        
        if (fraudCase.isClosed()) {
            throw new IllegalStateException("Case is already closed");
        }

        fraudCase.approve(reviewerId, notes);

        // Release hold if exists
        if (fraudCase.getHoldId() != null) {
            try {
                holdService.releaseHold(fraudCase.getHoldId(), reviewerId, "Fraud case approved");
            } catch (Exception e) {
                log.warn("Could not release hold: {}", e.getMessage());
            }
        }

        return fraudCaseRepository.save(fraudCase);
    }

    /**
     * Decline transaction (confirm fraud)
     */
    @Transactional
    public FraudCase declineTransaction(UUID caseId, UUID reviewerId, String notes, 
                                        boolean reverseTransaction, boolean freezeAccount) {
        log.info("Declining fraud case {}, reverse={}, freeze={}", caseId, reverseTransaction, freezeAccount);

        FraudCase fraudCase = getCaseById(caseId);
        
        if (fraudCase.isClosed()) {
            throw new IllegalStateException("Case is already closed");
        }

        fraudCase.decline(reviewerId, notes);

        // Capture hold if exists (confirm the debit)
        if (fraudCase.getHoldId() != null) {
            try {
                holdService.captureHold(fraudCase.getHoldId());
            } catch (Exception e) {
                log.warn("Could not capture hold: {}", e.getMessage());
            }
        }

        // Reverse transaction if requested
        if (reverseTransaction && fraudCase.getTransactionId() != null) {
            fraudCase.setTransactionReversed(true);
            fraudCase.setAccountActionTaken("Transaction reversed");
            // Transaction reversal would be handled by TransactionService
        }

        // Freeze account if requested
        if (freezeAccount) {
            try {
                Account account = accountRepository.findById(fraudCase.getAccountId()).orElse(null);
                if (account != null) {
                    account.setStatus(Account.AccountStatus.FROZEN);
                    accountRepository.save(account);
                    fraudCase.setAccountActionTaken(
                            (fraudCase.getAccountActionTaken() != null ? 
                                    fraudCase.getAccountActionTaken() + ", " : "") + "Account frozen");
                }
            } catch (Exception e) {
                log.error("Could not freeze account: {}", e.getMessage());
            }
        }

        return fraudCaseRepository.save(fraudCase);
    }

    /**
     * Escalate case
     */
    @Transactional
    public FraudCase escalateCase(UUID caseId, UUID escalateToId, String notes) {
        log.info("Escalating fraud case {} to {}", caseId, escalateToId);

        FraudCase fraudCase = getCaseById(caseId);
        fraudCase.escalate(escalateToId);
        fraudCase.setNotes((fraudCase.getNotes() != null ? fraudCase.getNotes() + "\n" : "") + 
                          "Escalated: " + notes);

        return fraudCaseRepository.save(fraudCase);
    }

    /**
     * Get cases assigned to user
     */
    @Transactional(readOnly = true)
    public List<FraudCase> getAssignedCases(UUID userId) {
        return fraudCaseRepository.findAssignedCases(userId);
    }

    /**
     * Get escalated cases
     */
    @Transactional(readOnly = true)
    public List<FraudCase> getEscalatedCases() {
        return fraudCaseRepository.findEscalatedCases();
    }

    /**
     * Get case statistics
     */
    @Transactional(readOnly = true)
    public FraudCaseStats getCaseStats() {
        return FraudCaseStats.builder()
                .openCases(fraudCaseRepository.countOpenCases())
                .criticalCases(fraudCaseRepository.countCriticalCases())
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class FraudCaseStats {
        private long openCases;
        private long criticalCases;
    }
}
