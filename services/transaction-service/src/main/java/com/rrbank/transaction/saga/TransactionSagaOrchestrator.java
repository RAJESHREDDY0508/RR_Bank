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
        log.info("Starting DEPOSIT SAGA for transaction: {}", transaction.getId());
        
        try {
            transaction.markProcessing();
            transactionRepository.save(transaction);
            eventProducer.publishTransactionInitiated(transaction);

            FraudCheckResponse fraudResponse = fraudClient.checkTransaction(
                    FraudCheckRequest.builder()
                            .accountId(transaction.getToAccountId())
                            .userId(userId)
                            .transactionType("DEPOSIT")
                            .amount(transaction.getAmount())
                            .build()
            );

            if ("REJECT".equals(fraudResponse.getDecision())) {
                throw new RuntimeException("Transaction rejected by fraud check: " + fraudResponse.getReason());
            }

            ledgerClient.credit(
                    transaction.getToAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getDescription()
            );

            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);
            eventProducer.publishTransactionCompleted(transaction);

            log.info("DEPOSIT SAGA completed successfully for: {}", transaction.getId());
            return transaction;

        } catch (Exception e) {
            log.error("DEPOSIT SAGA failed for: {}, error: {}", transaction.getId(), e.getMessage());
            return handleSagaFailure(transaction, e.getMessage());
        }
    }

    @Transactional
    public Transaction executeWithdrawal(Transaction transaction, UUID userId) {
        log.info("Starting WITHDRAWAL SAGA for transaction: {}", transaction.getId());

        try {
            transaction.markProcessing();
            transactionRepository.save(transaction);
            eventProducer.publishTransactionInitiated(transaction);

            FraudCheckResponse fraudResponse = fraudClient.checkTransaction(
                    FraudCheckRequest.builder()
                            .accountId(transaction.getFromAccountId())
                            .userId(userId)
                            .transactionType("WITHDRAWAL")
                            .amount(transaction.getAmount())
                            .build()
            );

            if ("REJECT".equals(fraudResponse.getDecision())) {
                throw new RuntimeException("Transaction rejected by fraud check: " + fraudResponse.getReason());
            }

            ledgerClient.debit(
                    transaction.getFromAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    transaction.getDescription()
            );

            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);
            eventProducer.publishTransactionCompleted(transaction);

            log.info("WITHDRAWAL SAGA completed successfully for: {}", transaction.getId());
            return transaction;

        } catch (Exception e) {
            log.error("WITHDRAWAL SAGA failed for: {}, error: {}", transaction.getId(), e.getMessage());
            return handleSagaFailure(transaction, e.getMessage());
        }
    }

    @Transactional
    public Transaction executeTransfer(Transaction transaction, UUID userId) {
        log.info("Starting TRANSFER SAGA for transaction: {}", transaction.getId());
        boolean debitCompleted = false;

        try {
            transaction.markProcessing();
            transactionRepository.save(transaction);
            eventProducer.publishTransactionInitiated(transaction);

            FraudCheckResponse fraudResponse = fraudClient.checkTransaction(
                    FraudCheckRequest.builder()
                            .accountId(transaction.getFromAccountId())
                            .userId(userId)
                            .transactionType("TRANSFER")
                            .amount(transaction.getAmount())
                            .build()
            );

            if ("REJECT".equals(fraudResponse.getDecision())) {
                throw new RuntimeException("Transaction rejected by fraud check: " + fraudResponse.getReason());
            }

            ledgerClient.debit(
                    transaction.getFromAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    "Transfer to " + transaction.getToAccountId()
            );
            debitCompleted = true;

            ledgerClient.credit(
                    transaction.getToAccountId(),
                    transaction.getId(),
                    transaction.getAmount(),
                    "Transfer from " + transaction.getFromAccountId()
            );

            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);
            eventProducer.publishTransactionCompleted(transaction);

            log.info("TRANSFER SAGA completed successfully for: {}", transaction.getId());
            return transaction;

        } catch (Exception e) {
            log.error("TRANSFER SAGA failed for: {}, error: {}", transaction.getId(), e.getMessage());
            
            if (debitCompleted) {
                log.info("SAGA Compensation: Reversing debit for failed transfer");
                try {
                    ledgerClient.credit(
                            transaction.getFromAccountId(),
                            transaction.getId(),
                            transaction.getAmount(),
                            "Reversal: Transfer failed"
                    );
                    log.info("SAGA Compensation completed");
                } catch (Exception ce) {
                    log.error("SAGA Compensation failed: {}", ce.getMessage());
                }
            }
            
            return handleSagaFailure(transaction, e.getMessage());
        }
    }

    private Transaction handleSagaFailure(Transaction transaction, String reason) {
        transaction.markFailed(reason);
        transaction = transactionRepository.save(transaction);
        eventProducer.publishTransactionFailed(transaction);
        return transaction;
    }
}
