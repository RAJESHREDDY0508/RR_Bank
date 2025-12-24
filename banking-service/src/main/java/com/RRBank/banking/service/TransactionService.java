package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.Transaction;
import com.RRBank.banking.event.*;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.repository.TransactionRepository;
import com.RRBank.banking.util.TransactionReferenceGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Transaction Service Handles money transfers with Saga pattern and
 * compensating transactions
 */
@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountCacheService cacheService;
    private final TransactionReferenceGenerator referenceGenerator;
    
    @Autowired(required = false)
    private TransactionEventProducer eventProducer;
    
    @Autowired
    public TransactionService(TransactionRepository transactionRepository,
                             AccountRepository accountRepository,
                             AccountCacheService cacheService,
                             TransactionReferenceGenerator referenceGenerator) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.cacheService = cacheService;
        this.referenceGenerator = referenceGenerator;
    }

    /**
     * Transfer money between accounts (Saga Pattern) This implements a
     * distributed transaction pattern with compensating actions
     *
     * Supports:
     * - TRANSFER: Both fromAccountId and toAccountId required
     * - DEPOSIT: Only toAccountId required (fromAccountId = null)
     * - WITHDRAWAL: Only fromAccountId required (toAccountId = null)
     *
     * ✅ FIX: Uses pessimistic locking instead of SERIALIZABLE isolation for
     * better performance
     */
    @Transactional
    public TransactionResponseDto transfer(TransferRequestDto request, UUID initiatedBy) {
        log.info("Initiating transaction from {} to {} amount: {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // Step 1: Check for duplicate transaction (Idempotency)
        if (request.getIdempotencyKey() != null) {
            Transaction existingTransaction = transactionRepository
                    .findByIdempotencyKey(request.getIdempotencyKey())
                    .orElse(null);

            if (existingTransaction != null) {
                log.warn("Duplicate transaction detected with idempotency key: {}", request.getIdempotencyKey());
                return TransactionResponseDto.fromEntity(existingTransaction);
            }
        }

        // Step 2: Determine transaction type and validate
        Transaction.TransactionType transactionType = determineTransactionType(request);
        
        // For transfers, validate accounts are different
        if (transactionType == Transaction.TransactionType.TRANSFER) {
            if (request.getFromAccountId() == null || request.getToAccountId() == null) {
                throw new IllegalArgumentException("Transfer requires both fromAccountId and toAccountId");
            }
            if (request.getFromAccountId().equals(request.getToAccountId())) {
                throw new IllegalArgumentException("Cannot transfer to the same account");
            }
        }

        // Step 3: Create transaction record (PENDING state)
        String transactionRef = referenceGenerator.generateTransactionReferenceWithType(transactionType.name());

        Transaction transaction = Transaction.builder()
                .transactionReference(transactionRef)
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .transactionType(transactionType)
                .amount(request.getAmount())
                .currency("USD")
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription())
                .idempotencyKey(request.getIdempotencyKey())
                .initiatedBy(initiatedBy)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction created with ID: {}, reference: {}", transaction.getId(), transactionRef);

        // Step 4: Publish Transaction Initiated Event
        publishTransactionInitiatedEvent(transaction);

        // Step 5: Execute Saga Pattern with compensating actions
        try {
            executeSagaTransfer(transaction, request.getAmount());

            // Mark transaction as completed
            transaction.markCompleted();
            transaction = transactionRepository.save(transaction);

            log.info("Transaction completed successfully: {}", transaction.getId());

            // Publish Transaction Completed Event
            publishTransactionCompletedEvent(transaction);

            return TransactionResponseDto.fromEntity(transaction);

        } catch (Exception e) {
            log.error("Transaction failed: {}, reason: {}", transaction.getId(), e.getMessage(), e);

            // Execute compensating transaction (rollback)
            compensateTransaction(transaction, e.getMessage());

            // Mark transaction as failed
            transaction.markFailed(e.getMessage());
            transaction = transactionRepository.save(transaction);

            // Publish Transaction Failed Event
            publishTransactionFailedEvent(transaction);

            throw new IllegalStateException("Transaction failed: " + e.getMessage(), e);
        }
    }

    /**
     * Determine transaction type based on request
     */
    private Transaction.TransactionType determineTransactionType(TransferRequestDto request) {
        // If explicitly specified in request, use that
        if (request.getTransactionType() != null) {
            try {
                return Transaction.TransactionType.valueOf(request.getTransactionType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transaction type: {}, auto-detecting", request.getTransactionType());
            }
        }
        
        // Auto-detect based on account IDs
        if (request.getFromAccountId() == null && request.getToAccountId() != null) {
            return Transaction.TransactionType.DEPOSIT;
        } else if (request.getFromAccountId() != null && request.getToAccountId() == null) {
            return Transaction.TransactionType.WITHDRAWAL;
        } else {
            return Transaction.TransactionType.TRANSFER;
        }
    }

    /**
     * Execute Saga Transfer (Main Transaction Flow)
     * Supports TRANSFER, DEPOSIT, and WITHDRAWAL
     */
    private void executeSagaTransfer(Transaction transaction, BigDecimal amount) {
        log.info("Executing saga {} for transaction: {}", 
                transaction.getTransactionType(), transaction.getId());

        // Update transaction status to PROCESSING
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);

        switch (transaction.getTransactionType()) {
            case DEPOSIT -> executeDeposit(transaction, amount);
            case WITHDRAWAL -> executeWithdrawal(transaction, amount);
            case TRANSFER -> executeTransfer(transaction, amount);
            default -> throw new IllegalStateException(
                    "Unsupported transaction type: " + transaction.getTransactionType());
        }
    }

    /**
     * Execute deposit (credit to account)
     */
    private void executeDeposit(Transaction transaction, BigDecimal amount) {
        Account toAccount = accountRepository.findByIdWithLock(transaction.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "To account not found: " + transaction.getToAccountId()));

        if (!toAccount.isActive()) {
            throw new IllegalStateException("To account is not active");
        }

        BigDecimal toAccountOldBalance = toAccount.getBalance();
        toAccount.credit(amount);
        
        // Update balance tracking on transaction
        transaction.setBalanceBefore(toAccountOldBalance);
        transaction.setBalanceAfter(toAccount.getBalance());
        transaction.setToAccountNumber(toAccount.getAccountNumber());
        
        accountRepository.save(toAccount);
        log.info("Deposited {} to account {}", amount, toAccount.getId());

        cacheService.updateCachedBalance(toAccount.getId(), toAccount.getBalance());
        publishBalanceUpdatedEvent(toAccount, toAccountOldBalance, "CREDIT", transaction.getId().toString());
    }

    /**
     * Execute withdrawal (debit from account)
     */
    private void executeWithdrawal(Transaction transaction, BigDecimal amount) {
        Account fromAccount = accountRepository.findByIdWithLock(transaction.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "From account not found: " + transaction.getFromAccountId()));

        if (!fromAccount.isActive()) {
            throw new IllegalStateException("From account is not active");
        }

        if (!fromAccount.hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance in account");
        }

        BigDecimal fromAccountOldBalance = fromAccount.getBalance();
        fromAccount.debit(amount);
        
        // Update balance tracking on transaction
        transaction.setBalanceBefore(fromAccountOldBalance);
        transaction.setBalanceAfter(fromAccount.getBalance());
        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
        
        accountRepository.save(fromAccount);
        log.info("Withdrew {} from account {}", amount, fromAccount.getId());

        cacheService.updateCachedBalance(fromAccount.getId(), fromAccount.getBalance());
        publishBalanceUpdatedEvent(fromAccount, fromAccountOldBalance, "DEBIT", transaction.getId().toString());
    }

    /**
     * Execute transfer (debit from source, credit to destination)
     */
    private void executeTransfer(Transaction transaction, BigDecimal amount) {
        // Step 1: Lock and validate FROM account
        Account fromAccount = accountRepository.findByIdWithLock(transaction.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                "From account not found: " + transaction.getFromAccountId()));

        // Step 2: Lock and validate TO account
        Account toAccount = accountRepository.findByIdWithLock(transaction.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                "To account not found: " + transaction.getToAccountId()));

        // Step 3: Validate accounts are active
        if (!fromAccount.isActive()) {
            throw new IllegalStateException("From account is not active");
        }
        if (!toAccount.isActive()) {
            throw new IllegalStateException("To account is not active");
        }

        // Step 4: Check sufficient balance
        if (!fromAccount.hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance in from account");
        }

        // Step 5: Validate currency match
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalStateException("Currency mismatch between accounts");
        }

        // Step 6: Debit from source account
        BigDecimal fromAccountOldBalance = fromAccount.getBalance();
        fromAccount.debit(amount);
        
        // Update account numbers on transaction
        transaction.setFromAccountNumber(fromAccount.getAccountNumber());
        transaction.setToAccountNumber(toAccount.getAccountNumber());
        
        accountRepository.save(fromAccount);
        log.info("Debited {} from account {}", amount, fromAccount.getId());

        // Invalidate cache for from account
        cacheService.updateCachedBalance(fromAccount.getId(), fromAccount.getBalance());

        // Publish balance updated event for from account
        publishBalanceUpdatedEvent(fromAccount, fromAccountOldBalance, "DEBIT", transaction.getId().toString());

        // Step 7: Credit to destination account
        BigDecimal toAccountOldBalance = toAccount.getBalance();
        toAccount.credit(amount);
        accountRepository.save(toAccount);
        log.info("Credited {} to account {}", amount, toAccount.getId());

        // Invalidate cache for to account
        cacheService.updateCachedBalance(toAccount.getId(), toAccount.getBalance());

        // Publish balance updated event for to account
        publishBalanceUpdatedEvent(toAccount, toAccountOldBalance, "CREDIT", transaction.getId().toString());
    }

    /**
     * Compensating Transaction (Rollback on Failure)
     * Handles compensation for TRANSFER, DEPOSIT, and WITHDRAWAL
     */
    private void compensateTransaction(Transaction transaction, String reason) {
        log.warn("Executing compensating transaction for: {}, type: {}, reason: {}", 
                transaction.getId(), transaction.getTransactionType(), reason);

        try {
            switch (transaction.getTransactionType()) {
                case DEPOSIT -> {
                    // For deposits, if we credited, we need to debit back
                    if (transaction.getToAccountId() != null) {
                        Account toAccount = accountRepository.findByIdWithLock(transaction.getToAccountId())
                                .orElse(null);
                        if (toAccount != null && transaction.getStatus() == Transaction.TransactionStatus.PROCESSING) {
                            BigDecimal oldBalance = toAccount.getBalance();
                            toAccount.debit(transaction.getAmount());
                            accountRepository.save(toAccount);
                            cacheService.updateCachedBalance(toAccount.getId(), toAccount.getBalance());
                            publishBalanceUpdatedEvent(toAccount, oldBalance, "DEBIT",
                                    transaction.getId().toString() + "-COMPENSATION");
                            log.info("Compensated deposit: debited {} from account {}",
                                    transaction.getAmount(), toAccount.getId());
                        }
                    }
                }
                case WITHDRAWAL -> {
                    // For withdrawals, if we debited, we need to credit back
                    if (transaction.getFromAccountId() != null) {
                        Account fromAccount = accountRepository.findByIdWithLock(transaction.getFromAccountId())
                                .orElse(null);
                        if (fromAccount != null) {
                            BigDecimal oldBalance = fromAccount.getBalance();
                            fromAccount.credit(transaction.getAmount());
                            accountRepository.save(fromAccount);
                            cacheService.updateCachedBalance(fromAccount.getId(), fromAccount.getBalance());
                            publishBalanceUpdatedEvent(fromAccount, oldBalance, "CREDIT",
                                    transaction.getId().toString() + "-COMPENSATION");
                            log.info("Compensated withdrawal: credited {} to account {}",
                                    transaction.getAmount(), fromAccount.getId());
                        }
                    }
                }
                case TRANSFER -> {
                    // For transfers, if we've debited the from account but failed to credit the to account,
                    // we need to refund the from account
                    if (transaction.getFromAccountId() != null) {
                        Account fromAccount = accountRepository.findByIdWithLock(transaction.getFromAccountId())
                                .orElse(null);
                        if (fromAccount != null) {
                            BigDecimal oldBalance = fromAccount.getBalance();
                            fromAccount.credit(transaction.getAmount());
                            accountRepository.save(fromAccount);
                            cacheService.updateCachedBalance(fromAccount.getId(), fromAccount.getBalance());
                            publishBalanceUpdatedEvent(fromAccount, oldBalance, "CREDIT",
                                    transaction.getId().toString() + "-COMPENSATION");
                            log.info("Compensated transfer: refunded {} to account {}",
                                    transaction.getAmount(), fromAccount.getId());
                        }
                    }
                }
                default -> log.warn("No compensation logic for transaction type: {}", 
                        transaction.getTransactionType());
            }

            // Mark transaction as reversed
            transaction.markReversed(reason);
            transactionRepository.save(transaction);

        } catch (Exception e) {
            log.error("Failed to execute compensating transaction for: {}", transaction.getId(), e);
            // In a real system, this would trigger alerts for manual intervention
        }
    }

    /**
     * Get transaction by ID
     */
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(UUID transactionId) {
        log.info("Fetching transaction with ID: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        return enrichTransactionResponse(transaction);
    }

    /**
     * Get transaction by reference
     */
    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByReference(String reference) {
        log.info("Fetching transaction with reference: {}", reference);

        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with reference: " + reference));

        return enrichTransactionResponse(transaction);
    }

    /**
     * Get all transactions for an account (with pagination)
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByAccountId(UUID accountId, Pageable pageable) {
        log.info("Fetching transactions for accountId: {} with pagination", accountId);

        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);
        return transactions.map(this::enrichTransactionResponse);
    }

    /**
     * Get all transactions for an account (non-paginated - deprecated, use
     * paginated version)
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByAccountId(UUID accountId) {
        log.info("Fetching transactions for accountId: {}", accountId);

        return transactionRepository.findByAccountId(accountId).stream()
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get recent transactions for account
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getRecentTransactions(UUID accountId, int limit) {
        log.info("Fetching {} recent transactions for accountId: {}", limit, accountId);

        return transactionRepository.findRecentTransactionsByAccountId(accountId, limit).stream()
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search transactions with filters (with pagination)
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> searchTransactions(TransactionSearchDto searchDto, Pageable pageable) {
        log.info("Searching transactions with filters and pagination");

        List<Transaction> transactions;

        if (searchDto.getAccountId() != null) {
            if (searchDto.getStartDate() != null && searchDto.getEndDate() != null) {
                transactions = transactionRepository.findByAccountIdAndDateRange(
                        searchDto.getAccountId(), searchDto.getStartDate(), searchDto.getEndDate());
            } else {
                transactions = transactionRepository.findByAccountId(searchDto.getAccountId());
            }
        } else if (searchDto.getStartDate() != null && searchDto.getEndDate() != null) {
            transactions = transactionRepository.findByDateRange(searchDto.getStartDate(), searchDto.getEndDate());
        } else {
            transactions = transactionRepository.findAll();
        }

        // Apply additional filters
        List<TransactionResponseDto> filtered = transactions.stream()
                .filter(t -> filterByStatus(t, searchDto.getStatuses()))
                .filter(t -> filterByType(t, searchDto.getTypes()))
                .filter(t -> filterByAmount(t, searchDto.getMinAmount(), searchDto.getMaxAmount()))
                .filter(t -> filterByDescription(t, searchDto.getDescription()))
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        return new PageImpl<>(
                filtered.subList(start, end),
                pageable,
                filtered.size()
        );
    }

    /**
     * Search transactions with filters (non-paginated - deprecated)
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> searchTransactions(TransactionSearchDto searchDto) {
        log.info("Searching transactions with filters");

        List<Transaction> transactions;

        if (searchDto.getAccountId() != null) {
            if (searchDto.getStartDate() != null && searchDto.getEndDate() != null) {
                transactions = transactionRepository.findByAccountIdAndDateRange(
                        searchDto.getAccountId(), searchDto.getStartDate(), searchDto.getEndDate());
            } else {
                transactions = transactionRepository.findByAccountId(searchDto.getAccountId());
            }
        } else if (searchDto.getStartDate() != null && searchDto.getEndDate() != null) {
            transactions = transactionRepository.findByDateRange(searchDto.getStartDate(), searchDto.getEndDate());
        } else {
            transactions = transactionRepository.findAll();
        }

        // Apply additional filters
        return transactions.stream()
                .filter(t -> filterByStatus(t, searchDto.getStatuses()))
                .filter(t -> filterByType(t, searchDto.getTypes()))
                .filter(t -> filterByAmount(t, searchDto.getMinAmount(), searchDto.getMaxAmount()))
                .filter(t -> filterByDescription(t, searchDto.getDescription()))
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all transactions (Admin) - with pagination
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getAllTransactions(Pageable pageable) {
        log.info("Fetching all transactions with pagination");

        Page<Transaction> transactions = transactionRepository.findAll(pageable);
        return transactions.map(this::enrichTransactionResponse);
    }

    /**
     * Get all transactions (Admin) - non-paginated (deprecated)
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getAllTransactions() {
        log.info("Fetching all transactions");

        return transactionRepository.findAll().stream()
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction statistics for account
     */
    @Transactional(readOnly = true)
    public TransactionStatsDto getTransactionStats(UUID accountId) {
        log.info("Calculating transaction statistics for accountId: {}", accountId);

        BigDecimal totalOutgoing = transactionRepository.getTotalOutgoingAmount(accountId);
        BigDecimal totalIncoming = transactionRepository.getTotalIncomingAmount(accountId);
        long transactionCount = transactionRepository.countByAccountId(accountId);

        return TransactionStatsDto.builder()
                .accountId(accountId)
                .totalOutgoing(totalOutgoing)
                .totalIncoming(totalIncoming)
                .netAmount(totalIncoming.subtract(totalOutgoing))
                .transactionCount(transactionCount)
                .build();
    }

    // ========== HELPER METHODS ==========
    private TransactionResponseDto enrichTransactionResponse(Transaction transaction) {
        TransactionResponseDto dto = TransactionResponseDto.fromEntity(transaction);

        // Enrich with account numbers
        if (transaction.getFromAccountId() != null) {
            accountRepository.findById(transaction.getFromAccountId())
                    .ifPresent(acc -> dto.setFromAccountNumber(acc.getAccountNumber()));
        }

        if (transaction.getToAccountId() != null) {
            accountRepository.findById(transaction.getToAccountId())
                    .ifPresent(acc -> dto.setToAccountNumber(acc.getAccountNumber()));
        }

        return dto;
    }

    private boolean filterByStatus(Transaction transaction, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return true;
        }
        return statuses.contains(transaction.getStatus().name());
    }

    private boolean filterByType(Transaction transaction, List<String> types) {
        if (types == null || types.isEmpty()) {
            return true;
        }
        return types.contains(transaction.getTransactionType().name());
    }

    private boolean filterByAmount(Transaction transaction, BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && transaction.getAmount().compareTo(minAmount) < 0) {
            return false;
        }
        if (maxAmount != null && transaction.getAmount().compareTo(maxAmount) > 0) {
            return false;
        }
        return true;
    }

    private boolean filterByDescription(Transaction transaction, String description) {
        if (description == null || description.isEmpty()) {
            return true;
        }
        return transaction.getDescription() != null
                && transaction.getDescription().toLowerCase().contains(description.toLowerCase());
    }

    // ========== EVENT PUBLISHING ==========
    private void publishTransactionInitiatedEvent(Transaction transaction) {
        TransactionInitiatedEvent event = TransactionInitiatedEvent.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .initiatedBy(transaction.getInitiatedBy())
                .initiatedAt(transaction.getCreatedAt())
                .build();

        if (eventProducer != null) {
            eventProducer.publishTransactionInitiated(event);
        }
    }

    private void publishTransactionCompletedEvent(Transaction transaction) {
        // Get updated balances
        BigDecimal fromBalance = accountRepository.findById(transaction.getFromAccountId())
                .map(Account::getBalance)
                .orElse(null);
        BigDecimal toBalance = accountRepository.findById(transaction.getToAccountId())
                .map(Account::getBalance)
                .orElse(null);

        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .fromAccountNewBalance(fromBalance)
                .toAccountNewBalance(toBalance)
                .completedAt(transaction.getCompletedAt())
                .build();

        if (eventProducer != null) {
            eventProducer.publishTransactionCompleted(event);
        }
    }

    private void publishTransactionFailedEvent(Transaction transaction) {
        TransactionFailedEvent event = TransactionFailedEvent.builder()
                .transactionId(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccountId(transaction.getFromAccountId())
                .toAccountId(transaction.getToAccountId())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .failureReason(transaction.getFailureReason())
                .failedAt(transaction.getFailedAt())
                .build();

        if (eventProducer != null) {
            eventProducer.publishTransactionFailed(event);
        }
    }

    private void publishBalanceUpdatedEvent(Account account, BigDecimal oldBalance,
            String transactionType, String transactionId) {
        BigDecimal changeAmount = account.getBalance().subtract(oldBalance);

        BalanceUpdatedEvent event = BalanceUpdatedEvent.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomerId())
                .oldBalance(oldBalance)
                .newBalance(account.getBalance())
                .changeAmount(changeAmount)
                .transactionType(transactionType)
                .transactionId(transactionId)
                .updatedAt(LocalDateTime.now())
                .build();

        // This would be published via AccountEventProducer
        // For now, we'll log it
        log.info("Balance updated for account {}: {} -> {}",
                account.getId(), oldBalance, account.getBalance());
    }
}
