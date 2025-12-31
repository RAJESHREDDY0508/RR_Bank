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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Transaction Service - Handles deposits, withdrawals, and transfers
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

    // ========== DEPOSIT API ==========

    @Transactional
    public TransactionResponseDto deposit(UUID accountId, BigDecimal amount, String description,
                                         String idempotencyKey, UUID initiatedBy) {
        log.info("Processing deposit of {} to accountId: {}", amount, accountId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // Check for duplicate transaction
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                log.warn("Duplicate deposit detected with idempotency key: {}", idempotencyKey);
                return TransactionResponseDto.fromEntity(existing);
            }
        }

        // Get account with lock
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.isActive()) {
            throw new IllegalStateException("Cannot deposit to inactive account");
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        String transactionRef = referenceGenerator.generateTransactionReferenceWithType("DEPOSIT");

        Transaction transaction = Transaction.builder()
                .transactionReference(transactionRef)
                .toAccountId(accountId)
                .toAccountNumber(account.getAccountNumber())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .currency(account.getCurrency())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(description != null ? description : "Deposit")
                .idempotencyKey(idempotencyKey)
                .initiatedBy(initiatedBy)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();

        transaction.markCompleted();
        transaction = transactionRepository.save(transaction);

        // Update account balance
        account.credit(amount);
        accountRepository.save(account);

        // Update cache (safe - won't throw if Redis unavailable)
        try {
            cacheService.updateCachedBalance(accountId, account.getBalance());
        } catch (Exception e) {
            log.debug("Cache update failed, continuing: {}", e.getMessage());
        }

        log.info("Deposit completed: {} to account {}, balance: {} -> {}",
                amount, accountId, balanceBefore, balanceAfter);

        return TransactionResponseDto.fromEntity(transaction);
    }

    // ========== WITHDRAWAL API ==========

    @Transactional
    public TransactionResponseDto withdraw(UUID accountId, BigDecimal amount, String description,
                                          String idempotencyKey, UUID initiatedBy) {
        log.info("Processing withdrawal of {} from accountId: {}", amount, accountId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        // Check for duplicate
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                log.warn("Duplicate withdrawal detected with idempotency key: {}", idempotencyKey);
                return TransactionResponseDto.fromEntity(existing);
            }
        }

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        if (!account.isActive()) {
            throw new IllegalStateException("Cannot withdraw from inactive account");
        }

        if (!account.hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance. Available: " + account.getBalance());
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        String transactionRef = referenceGenerator.generateTransactionReferenceWithType("WITHDRAWAL");

        Transaction transaction = Transaction.builder()
                .transactionReference(transactionRef)
                .fromAccountId(accountId)
                .fromAccountNumber(account.getAccountNumber())
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .currency(account.getCurrency())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(description != null ? description : "Withdrawal")
                .idempotencyKey(idempotencyKey)
                .initiatedBy(initiatedBy)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();

        transaction.markCompleted();
        transaction = transactionRepository.save(transaction);

        account.debit(amount);
        accountRepository.save(account);

        try {
            cacheService.updateCachedBalance(accountId, account.getBalance());
        } catch (Exception e) {
            log.debug("Cache update failed, continuing: {}", e.getMessage());
        }

        log.info("Withdrawal completed: {} from account {}, balance: {} -> {}",
                amount, accountId, balanceBefore, balanceAfter);

        return TransactionResponseDto.fromEntity(transaction);
    }

    // ========== INITIAL DEPOSIT ==========

    @Transactional
    public TransactionResponseDto createInitialDeposit(UUID accountId, BigDecimal amount, UUID initiatedBy) {
        log.info("Creating initial deposit of {} for accountId: {}", amount, accountId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        String transactionRef = referenceGenerator.generateTransactionReferenceWithType("DEPOSIT");

        Transaction transaction = Transaction.builder()
                .transactionReference(transactionRef)
                .toAccountId(accountId)
                .toAccountNumber(account.getAccountNumber())
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(amount)
                .currency(account.getCurrency())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Initial Deposit")
                .initiatedBy(initiatedBy)
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(amount)
                .build();

        transaction.markCompleted();
        transaction = transactionRepository.save(transaction);

        log.info("Initial deposit created: {} for account {}", transaction.getId(), accountId);

        return TransactionResponseDto.fromEntity(transaction);
    }

    // ========== TRANSFER API ==========

    @Transactional
    public TransactionResponseDto transfer(TransferRequestDto request, UUID initiatedBy) {
        log.info("Initiating transfer from {} to {} amount: {}",
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (request.getFromAccountId() == null || request.getToAccountId() == null) {
            throw new IllegalArgumentException("Transfer requires both fromAccountId and toAccountId");
        }
        
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Check for duplicate
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Transaction existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
            if (existing != null) {
                log.warn("Duplicate transfer detected with idempotency key: {}", request.getIdempotencyKey());
                return TransactionResponseDto.fromEntity(existing);
            }
        }

        // Deterministic lock ordering to prevent deadlocks
        UUID firstLock = request.getFromAccountId().compareTo(request.getToAccountId()) < 0 
                ? request.getFromAccountId() : request.getToAccountId();
        UUID secondLock = request.getFromAccountId().compareTo(request.getToAccountId()) < 0 
                ? request.getToAccountId() : request.getFromAccountId();

        Account firstAccount = accountRepository.findByIdWithLock(firstLock)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + firstLock));
        Account secondAccount = accountRepository.findByIdWithLock(secondLock)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + secondLock));

        Account fromAccount = request.getFromAccountId().equals(firstLock) ? firstAccount : secondAccount;
        Account toAccount = request.getToAccountId().equals(firstLock) ? firstAccount : secondAccount;

        if (!fromAccount.isActive()) {
            throw new IllegalStateException("Source account is not active");
        }
        if (!toAccount.isActive()) {
            throw new IllegalStateException("Destination account is not active");
        }
        if (!fromAccount.hasSufficientBalance(request.getAmount())) {
            throw new IllegalStateException("Insufficient balance. Available: " + fromAccount.getBalance());
        }
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new IllegalStateException("Currency mismatch between accounts");
        }

        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        BigDecimal toBalanceBefore = toAccount.getBalance();

        fromAccount.debit(request.getAmount());
        toAccount.credit(request.getAmount());

        BigDecimal fromBalanceAfter = fromAccount.getBalance();
        BigDecimal toBalanceAfter = toAccount.getBalance();

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Create TRANSFER_OUT
        String transferOutRef = referenceGenerator.generateTransactionReferenceWithType("TRANSFER_OUT");
        Transaction transferOut = Transaction.builder()
                .transactionReference(transferOutRef)
                .fromAccountId(fromAccount.getId())
                .fromAccountNumber(fromAccount.getAccountNumber())
                .toAccountId(toAccount.getId())
                .toAccountNumber(toAccount.getAccountNumber())
                .transactionType(Transaction.TransactionType.TRANSFER_OUT)
                .amount(request.getAmount())
                .currency(fromAccount.getCurrency())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : 
                        "Transfer to " + toAccount.getAccountNumber())
                .idempotencyKey(request.getIdempotencyKey())
                .initiatedBy(initiatedBy)
                .balanceBefore(fromBalanceBefore)
                .balanceAfter(fromBalanceAfter)
                .build();
        transferOut.markCompleted();
        transferOut = transactionRepository.save(transferOut);

        // Create TRANSFER_IN
        String transferInRef = referenceGenerator.generateTransactionReferenceWithType("TRANSFER_IN");
        Transaction transferIn = Transaction.builder()
                .transactionReference(transferInRef)
                .fromAccountId(fromAccount.getId())
                .fromAccountNumber(fromAccount.getAccountNumber())
                .toAccountId(toAccount.getId())
                .toAccountNumber(toAccount.getAccountNumber())
                .transactionType(Transaction.TransactionType.TRANSFER_IN)
                .amount(request.getAmount())
                .currency(toAccount.getCurrency())
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : 
                        "Transfer from " + fromAccount.getAccountNumber())
                .initiatedBy(initiatedBy)
                .balanceBefore(toBalanceBefore)
                .balanceAfter(toBalanceAfter)
                .build();
        transferIn.markCompleted();
        transactionRepository.save(transferIn);

        try {
            cacheService.updateCachedBalance(fromAccount.getId(), fromAccount.getBalance());
            cacheService.updateCachedBalance(toAccount.getId(), toAccount.getBalance());
        } catch (Exception e) {
            log.debug("Cache update failed, continuing: {}", e.getMessage());
        }

        log.info("Transfer completed: {} from {} to {}", 
                request.getAmount(), fromAccount.getAccountNumber(), toAccount.getAccountNumber());

        return TransactionResponseDto.fromEntity(transferOut);
    }

    // ========== READ OPERATIONS ==========

    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionById(UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
        return enrichTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponseDto getTransactionByReference(String reference) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));
        return enrichTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getTransactionsByAccountId(UUID accountId, Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);
        return transactions.map(this::enrichTransactionResponse);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getRecentTransactions(UUID accountId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Transaction> transactions = transactionRepository.findRecentByAccountId(accountId, pageable);
        return transactions.stream()
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> searchTransactions(TransactionSearchDto searchDto, Pageable pageable) {
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

        List<TransactionResponseDto> filtered = transactions.stream()
                .filter(t -> filterByStatus(t, searchDto.getStatuses()))
                .filter(t -> filterByType(t, searchDto.getTypes()))
                .filter(t -> filterByAmount(t, searchDto.getMinAmount(), searchDto.getMaxAmount()))
                .filter(t -> filterByDescription(t, searchDto.getDescription()))
                .map(this::enrichTransactionResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        
        if (start > filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }

        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findAll(pageable);
        return transactions.map(this::enrichTransactionResponse);
    }

    @Transactional(readOnly = true)
    public TransactionStatsDto getTransactionStats(UUID accountId) {
        BigDecimal totalOutgoing = transactionRepository.getTotalOutgoingAmount(accountId);
        BigDecimal totalIncoming = transactionRepository.getTotalIncomingAmount(accountId);
        long transactionCount = transactionRepository.countByAccountId(accountId);

        return TransactionStatsDto.builder()
                .accountId(accountId)
                .totalOutgoing(totalOutgoing != null ? totalOutgoing : BigDecimal.ZERO)
                .totalIncoming(totalIncoming != null ? totalIncoming : BigDecimal.ZERO)
                .netAmount((totalIncoming != null ? totalIncoming : BigDecimal.ZERO)
                        .subtract(totalOutgoing != null ? totalOutgoing : BigDecimal.ZERO))
                .transactionCount(transactionCount)
                .build();
    }

    // ========== HELPER METHODS ==========

    private TransactionResponseDto enrichTransactionResponse(Transaction transaction) {
        TransactionResponseDto dto = TransactionResponseDto.fromEntity(transaction);

        if (transaction.getFromAccountId() != null && dto.getFromAccountNumber() == null) {
            accountRepository.findById(transaction.getFromAccountId())
                    .ifPresent(acc -> dto.setFromAccountNumber(acc.getAccountNumber()));
        }

        if (transaction.getToAccountId() != null && dto.getToAccountNumber() == null) {
            accountRepository.findById(transaction.getToAccountId())
                    .ifPresent(acc -> dto.setToAccountNumber(acc.getAccountNumber()));
        }

        return dto;
    }

    private boolean filterByStatus(Transaction transaction, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) return true;
        return statuses.contains(transaction.getStatus().name());
    }

    private boolean filterByType(Transaction transaction, List<String> types) {
        if (types == null || types.isEmpty()) return true;
        return types.contains(transaction.getTransactionType().name());
    }

    private boolean filterByAmount(Transaction transaction, BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && transaction.getAmount().compareTo(minAmount) < 0) return false;
        if (maxAmount != null && transaction.getAmount().compareTo(maxAmount) > 0) return false;
        return true;
    }

    private boolean filterByDescription(Transaction transaction, String description) {
        if (description == null || description.isEmpty()) return true;
        return transaction.getDescription() != null
                && transaction.getDescription().toLowerCase().contains(description.toLowerCase());
    }
}
