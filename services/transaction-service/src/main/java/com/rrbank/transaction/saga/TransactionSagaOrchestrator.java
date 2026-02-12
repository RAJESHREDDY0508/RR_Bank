package com.rrbank.transaction.saga;

import com.rrbank.transaction.dto.TransactionDTOs.*;
import com.rrbank.transaction.entity.Transaction;
import com.rrbank.transaction.event.TransactionEventProducer;
import com.rrbank.transaction.repository.TransactionRepository;
import com.rrbank.transaction.service.FraudServiceClient;
import com.rrbank.transaction.service.LedgerServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionSagaOrchestrator {

    private final TransactionRepository transactionRepository;
    private final LedgerServiceClient ledgerClient;
    private final FraudServiceClient fraudClient;
    private final TransactionEventProducer eventProducer;

    @Transactional
    public Transaction executeDeposit(Transaction transaction, UUID userId) {
        log.info("=== Starting DEPOSIT SAGA for transaction: {} ===", transaction.getId());
        log.info("Deposit details: accountId={}, amount={}, userId={}", 
                transaction.getToAccountId(), transaction.getAmount(), userId);
        
        try {
            // Step 1: Mark as processing
            log.info("Step 1: Marking transaction as PROCESSING");
            transaction.markProcessing();
            transactionRepository.save(transaction);
            
            try {
                eventProducer.publishTransactionInitiated(transaction);
            } catch (Exception e) {
                log.warn("Failed to publish transaction initiated event (non-fatal): {}", e.getMessage());
            }

            // Step 2: Fraud check
            log.info("Step 2: Performing fraud check");
            FraudCheckResponse fraudResponse = fraudClient.checkTransaction(
                    FraudCheckRequest.builder()
                            .accountId(transaction.getToAccountId())
                            .userId(userId)
                            .transactionType("DEPOSIT")
                            .amount(transaction.getAmount())
                            .build()
            );
            log.info("Fraud check response: decision={}, reason={}", 
                    fraudResponse.getDecision(), fraudResponse.getReason());

            if ("REJECT".equals(fraudResponse.getDecision())) {
                throw new RuntimeException("Transaction rejected by fraud check: " + fraudResponse.getReason());
            }

            // Step 3: Credit ledger
            log.info("Step 3: Crediting ledger for account {}", transaction.getToAccountId());
            ledgerClient.credit(
                    transaction.getToAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getDescription()
            );
            log.info("Ledger credit successful");

            // Step 4: Mark completed
            log.info("Step 4: Marking transaction as COMPLETED");
            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);
            
            try {
                eventProducer.publishTransactionCompleted(transaction);
            } catch (Exception e) {
                log.warn("Failed to publish transaction completed event (non-fatal): {}", e.getMessage());
            }

            log.info("=== DEPOSIT SAGA completed successfully for: {} ===", transaction.getId());
            return transaction;

        } catch (Exception e) {
            log.error("=== DEPOSIT SAGA FAILED for: {} ===", transaction.getId());
            log.error("Error details: {}", e.getMessage(), e);
            return handleSagaFailure(transaction, e.getMessage());
        }
    }

    @Transactional
    public Transaction executeWithdrawal(Transaction transaction, UUID userId) {
        log.info("=== Starting WITHDRAWAL SAGA for transaction: {} ===", transaction.getId());
        log.info("Withdrawal details: accountId={}, amount={}, userId={}", 
                transaction.getFromAccountId(), transaction.getAmount(), userId);

        try {
            // Step 1: Mark as processing
            log.info("Step 1: Marking transaction as PROCESSING");
            transaction.markProcessing();
            transactionRepository.save(transaction);
            
            try {
                eventProducer.publishTransactionInitiated(transaction);
            } catch (Exception e) {
                log.warn("Failed to publish transaction initiated event (non-fatal): {}", e.getMessage());
            }

            // Step 2: Fraud check
            log.info("Step 2: Performing fraud check");
            FraudCheckResponse fraudResponse = fraudClient.checkTransaction(
                    FraudCheckRequest.builder()
                            .accountId(transaction.getFromAccountId())
                            .userId(userId)
                            .transactionType("WITHDRAWAL")
                            .amount(transaction.getAmount())
                            .build()
            );
            log.info("Fraud check response: decision={}, reason={}", 
                    fraudResponse.getDecision(), fraudResponse.getReason());

            if ("REJECT".equals(fraudResponse.getDecision())) {
                throw new RuntimeException("Transaction rejected by fraud check: " + fraudResponse.getReason());
            }

            // Step 3: Debit ledger
            log.info("Step 3: Debiting ledger for account {}", transaction.getFromAccountId());
            ledgerClient.debit(
                    transaction.getFromAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getDescription()
            );
            log.info("Ledger debit successful");

            // Step 4: Mark completed
            log.info("Step 4: Marking transaction as COMPLETED");
            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);
            
            try {
                eventProducer.publishTransactionCompleted(transaction);
            } catch (Exception e) {
                log.warn("Failed to publish transaction completed event (non-fatal): {}", e.getMessage());
            }

            log.info("=== WITHDRAWAL SAGA completed successfully for: {} ===", transaction.getId());
            return transaction;

        } catch (Exception e) {
            log.error("=== WITHDRAWAL SAGA FAILED for: {} ===", transaction.getId());
            log.error("Error details: {}", e.getMessage(), e);
            return handleSagaFailure(transaction, e.getMessage());
        }
    }

    @Transactional
    public Transaction executeTransfer(Transaction transaction, UUID userId) {
        log.info("=== Starting TRANSFER SAGA for transaction: {} ===", transaction.getId());
        log.info("Transfer details: from={}, to={}, amount={}, userId={}", 
                transaction.getFromAccountId(), transaction.getToAccountId(), 
                transaction.getAmount(), userId);
        
        boolean debitCompleted = false;

        try {
            // Step 1: Mark as processing
            log.info("Step 1: Marking transaction as PROCESSING");
            transaction.markProcessing();
            transactionRepository.save(transaction);
            
            try {
                eventProducer.publishTransactionInitiated(transaction);
            } catch (Exception e) {
                log.warn("Failed to publish transaction initiated event (non-fatal): {}", e.getMessage());
            }

            // Step 2: Fraud check
            log.info("Step 2: Performing fraud check");
            FraudCheckResponse fraudResponse = fraudClient.checkTransaction(
                    FraudCheckRequest.builder()
                            .accountId(transaction.getFromAccountId())
                            .userId(userId)
                            .transactionType("TRANSFER")
                            .amount(transaction.getAmount())
                            .build()
            );
            log.info("Fraud check response: decision={}, reason={}", 
                    fraudResponse.getDecision(), fraudResponse.getReason());

            if ("REJECT".equals(fraudResponse.getDecision())) {
                throw new RuntimeException("Transaction rejected by fraud check: " + fraudResponse.getReason());
            }

            // Step 3: Debit source account
            log.info("Step 3: Debiting source account {}", transaction.getFromAccountId());
            ledgerClient.debit(
                    transaction.getFromAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    "Transfer to " + transaction.getToAccountId()
            );
            debitCompleted = true;
            log.info("Debit successful");

            // Step 4: Credit destination account
            log.info("Step 4: Crediting destination account {}", transaction.getToAccountId());
            ledgerClient.credit(
                    transaction.getToAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    "Transfer from " + transaction.getFromAccountId()
            );
            log.info("Credit successful");

            // Step 5: Mark completed
            log.info("Step 5: Marking transaction as COMPLETED");
            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);
            
            try {
                eventProducer.publishTransactionCompleted(transaction);
            } catch (Exception e) {
                log.warn("Failed to publish transaction completed event (non-fatal): {}", e.getMessage());
            }

            log.info("=== TRANSFER SAGA completed successfully for: {} ===", transaction.getId());
            return transaction;

        } catch (Exception e) {
            log.error("=== TRANSFER SAGA FAILED for: {} ===", transaction.getId());
            log.error("Error details: {}", e.getMessage(), e);
            
            // Compensation: Reverse debit if it was completed
            if (debitCompleted) {
                log.info("SAGA Compensation: Reversing debit for failed transfer");
                try {
                    ledgerClient.credit(
                            transaction.getFromAccountId(),
                            transaction.getId(),
                            transaction.getAmount(),
                            "Reversal: Transfer failed - " + e.getMessage()
                    );
                    log.info("SAGA Compensation completed successfully");
                } catch (Exception ce) {
                    log.error("SAGA Compensation FAILED - Manual intervention required: {}", ce.getMessage());
                }
            }
            
            return handleSagaFailure(transaction, e.getMessage());
        }
    }

    private Transaction handleSagaFailure(Transaction transaction, String reason) {
        log.info("Handling saga failure: {}", reason);
        transaction.markFailed(reason);
        transaction = transactionRepository.save(transaction);
        
        try {
            eventProducer.publishTransactionFailed(transaction);
        } catch (Exception e) {
            log.warn("Failed to publish transaction failed event (non-fatal): {}", e.getMessage());
        }
        
        return transaction;
    }
}
