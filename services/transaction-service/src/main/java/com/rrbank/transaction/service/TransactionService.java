package com.rrbank.transaction.service;

import com.rrbank.transaction.dto.TransactionDTOs.*;
import com.rrbank.transaction.entity.Transaction;
import com.rrbank.transaction.exception.KycNotApprovedException;
import com.rrbank.transaction.repository.TransactionRepository;
import com.rrbank.transaction.saga.TransactionSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionSagaOrchestrator sagaOrchestrator;
    private final CustomerServiceClient customerServiceClient;

    @Transactional
    public TransactionResponse deposit(DepositRequest request, UUID userId) {
        log.info("Processing deposit request for account: {}", request.getAccountId());

        // KYC Verification Guard
        verifyKycApproved(userId);

        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing transaction for idempotency key: {}", request.getIdempotencyKey());
                return toResponse(existing.get());
            }
        }

        Transaction transaction = Transaction.builder()
                .toAccountId(request.getAccountId())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .description(request.getDescription() != null ? request.getDescription() : "Deposit")
                .idempotencyKey(request.getIdempotencyKey())
                .initiatedBy(userId)
                .build();
        transaction = transactionRepository.save(transaction);

        transaction = sagaOrchestrator.executeDeposit(transaction, userId);
        
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, UUID userId) {
        log.info("Processing withdrawal request for account: {}", request.getAccountId());

        // KYC Verification Guard
        verifyKycApproved(userId);

        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing transaction for idempotency key: {}", request.getIdempotencyKey());
                return toResponse(existing.get());
            }
        }

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getAccountId())
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
                .idempotencyKey(request.getIdempotencyKey())
                .initiatedBy(userId)
                .build();
        transaction = transactionRepository.save(transaction);

        transaction = sagaOrchestrator.executeWithdrawal(transaction, userId);
        
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request, UUID userId) {
        log.info("Processing transfer request from {} to {}", 
                request.getFromAccountId(), request.getToAccountId());

        // KYC Verification Guard
        verifyKycApproved(userId);

        if (request.getIdempotencyKey() != null) {
            Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing transaction for idempotency key: {}", request.getIdempotencyKey());
                return toResponse(existing.get());
            }
        }

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .transactionType(Transaction.TransactionType.TRANSFER)
                .amount(request.getAmount())
                .description(request.getDescription() != null ? request.getDescription() : "Transfer")
                .idempotencyKey(request.getIdempotencyKey())
                .initiatedBy(userId)
                .build();
        transaction = transactionRepository.save(transaction);

        transaction = sagaOrchestrator.executeTransfer(transaction, userId);
        
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + reference));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByAccount(UUID accountId, Pageable pageable) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
                accountId, accountId, pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByAccountAndDateRange(
            UUID accountId, 
            LocalDate startDate, 
            LocalDate endDate,
            String type,
            Pageable pageable) {
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();
        
        if (type != null && !type.isEmpty()) {
            try {
                Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
                return transactionRepository.findByAccountIdAndDateRangeAndType(
                        accountId, startDateTime, endDateTime, transactionType, pageable
                ).map(this::toResponse);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transaction type: {}", type);
            }
        }
        
        return transactionRepository.findByAccountIdAndDateRange(
                accountId, startDateTime, endDateTime, pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> exportTransactions(
            UUID accountId, 
            LocalDate startDate, 
            LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();
        
        return transactionRepository.findAllByAccountIdAndDateRange(accountId, startDateTime, endDateTime)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Verifies that the user's KYC is approved before allowing transactions.
     * @param userId The user ID to verify
     * @throws KycNotApprovedException if KYC is not approved
     */
    private void verifyKycApproved(UUID userId) {
        if (userId == null) {
            log.warn("No user ID provided for KYC verification, skipping check");
            return;
        }
        
        CustomerServiceClient.KycStatusResponse kycStatus = customerServiceClient.getKycStatus(userId);
        
        if (!kycStatus.isApproved()) {
            log.warn("Transaction blocked - KYC not approved for user: {}, status: {}", userId, kycStatus.kycStatus());
            
            String message;
            if (kycStatus.isRejected()) {
                message = "Your KYC verification was rejected. " + 
                         (kycStatus.rejectionReason() != null ? "Reason: " + kycStatus.rejectionReason() : 
                          "Please contact support for assistance.");
            } else {
                message = "Your account is pending KYC verification. Transactions will be enabled once admin approves your KYC.";
            }
            
            throw new KycNotApprovedException(kycStatus.kycStatus(), message);
        }
        
        log.debug("KYC verification passed for user: {}", userId);
    }

    private TransactionResponse toResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId().toString())
                .transactionReference(tx.getTransactionReference())
                .fromAccountId(tx.getFromAccountId() != null ? tx.getFromAccountId().toString() : null)
                .toAccountId(tx.getToAccountId() != null ? tx.getToAccountId().toString() : null)
                .transactionType(tx.getTransactionType().name())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .status(tx.getStatus().name())
                .description(tx.getDescription())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .completedAt(tx.getCompletedAt())
                .build();
    }
}
