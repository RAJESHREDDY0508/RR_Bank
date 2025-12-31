package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.service.OwnershipService;
import com.RRBank.banking.service.TransactionLimitService;
import com.RRBank.banking.service.TransactionService;
import com.RRBank.banking.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transaction Controller
 * REST API endpoints for transaction management
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionLimitService transactionLimitService;
    private final OwnershipService ownershipService;
    private final AccountRepository accountRepository;

    // ==================== DEPOSIT/WITHDRAW CONVENIENCE ENDPOINTS ====================
    // ✅ FIX: Added /api/transactions/deposit and /api/transactions/withdraw for frontend compatibility

    /**
     * Deposit money to account
     * POST /api/transactions/deposit
     * 
     * ✅ FIX: Alternative endpoint that some frontends expect
     */
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> depositViaTransactions(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositWithAccountRequest request,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(request.getAccountId(), request.getAccountNumber());
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to deposit {} to account: {} via /transactions/deposit", request.getAmount(), accountId);
        
        UUID initiatedBy = SecurityUtil.requireUserId(authentication);
        
        TransactionResponseDto response = transactionService.deposit(
                accountId,
                request.getAmount(),
                request.getDescription(),
                idempotencyKey,
                initiatedBy
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw money from account
     * POST /api/transactions/withdraw
     * 
     * ✅ FIX: Alternative endpoint that some frontends expect
     */
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> withdrawViaTransactions(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WithdrawWithAccountRequest request,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(request.getAccountId(), request.getAccountNumber());
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to withdraw {} from account: {} via /transactions/withdraw", request.getAmount(), accountId);
        
        UUID initiatedBy = SecurityUtil.requireUserId(authentication);
        
        TransactionResponseDto response = transactionService.withdraw(
                accountId,
                request.getAmount(),
                request.getDescription(),
                idempotencyKey,
                initiatedBy
        );
        
        return ResponseEntity.ok(response);
    }

    // ==================== TRANSFER ENDPOINT ====================

    /**
     * Transfer money between accounts
     * POST /api/transactions/transfer
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequestDto request,
            Authentication authentication) {
        
        // Resolve account numbers to IDs if provided
        if (request.getFromAccountId() == null && request.getFromAccountNumber() != null) {
            Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Source account not found: " + request.getFromAccountNumber()));
            request.setFromAccountId(fromAccount.getId());
        }
        
        if (request.getToAccountId() == null && request.getToAccountNumber() != null) {
            Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Destination account not found: " + request.getToAccountNumber()));
            request.setToAccountId(toAccount.getId());
        }
        
        // Validate we have both accounts
        if (request.getFromAccountId() == null) {
            throw new IllegalArgumentException("Source account is required");
        }
        if (request.getToAccountId() == null) {
            throw new IllegalArgumentException("Destination account is required");
        }
        
        // Ownership check
        ownershipService.requireAccountOwnership(request.getFromAccountId(), authentication);
        
        log.info("Transfer request: from {} to {} amount: {}", 
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        
        UUID userId = SecurityUtil.requireUserId(authentication);
        
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            request.setIdempotencyKey(idempotencyKey);
        }
        
        TransactionResponseDto response = transactionService.transfer(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get transaction limits
     * GET /api/transactions/limits
     */
    @GetMapping("/limits")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<TransactionLimitResponse>> getMyLimits(Authentication authentication) {
        String userId = SecurityUtil.requireUserIdString(authentication);
        List<TransactionLimitResponse> limits = transactionLimitService.getLimitsForUser(userId);
        return ResponseEntity.ok(limits);
    }

    /**
     * Get transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> getTransactionById(
            @PathVariable UUID id,
            Authentication authentication) {
        
        TransactionResponseDto response = transactionService.getTransactionById(id);
        
        // Ownership check
        if (!SecurityUtil.isAdmin(authentication)) {
            boolean hasAccess = false;
            if (response.getFromAccountId() != null) {
                hasAccess = ownershipService.canAccessAccount(response.getFromAccountId(), authentication);
            }
            if (!hasAccess && response.getToAccountId() != null) {
                hasAccess = ownershipService.canAccessAccount(response.getToAccountId(), authentication);
            }
            if (!hasAccess) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to view this transaction");
            }
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction by reference
     * GET /api/transactions/reference/{reference}
     */
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> getTransactionByReference(
            @PathVariable String reference,
            Authentication authentication) {
        
        TransactionResponseDto response = transactionService.getTransactionByReference(reference);
        
        if (!SecurityUtil.isAdmin(authentication)) {
            boolean hasAccess = false;
            if (response.getFromAccountId() != null) {
                hasAccess = ownershipService.canAccessAccount(response.getFromAccountId(), authentication);
            }
            if (!hasAccess && response.getToAccountId() != null) {
                hasAccess = ownershipService.canAccessAccount(response.getToAccountId(), authentication);
            }
            if (!hasAccess) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You do not have permission to view this transaction");
            }
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all transactions for an account
     * GET /api/transactions/account/{accountIdOrNumber}
     * 
     * Accepts either UUID or account number
     */
    @GetMapping("/account/{accountIdOrNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionsByAccount(
            @PathVariable String accountIdOrNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Authentication authentication) {
        
        // Try to parse as UUID first, if fails, treat as account number
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        
        log.info("Get transactions for account: {} - page: {}, size: {}", accountId, page, size);
        
        Sort sortObj = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        Page<TransactionResponseDto> transactions = transactionService.getTransactionsByAccountId(accountId, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get recent transactions for account
     * GET /api/transactions/account/{accountIdOrNumber}/recent
     */
    @GetMapping("/account/{accountIdOrNumber}/recent")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<TransactionResponseDto>> getRecentTransactions(
            @PathVariable String accountIdOrNumber,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        ownershipService.requireAccountOwnership(accountId, authentication);
        
        log.info("Get {} recent transactions for account: {}", limit, accountId);
        List<TransactionResponseDto> transactions = transactionService.getRecentTransactions(accountId, limit);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Search transactions with filters
     * GET /api/transactions/search
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionResponseDto>> searchTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort,
            Authentication authentication) {
        
        // Resolve account
        UUID resolvedAccountId = null;
        if (accountId != null) {
            resolvedAccountId = resolveAccountId(accountId);
        } else if (accountNumber != null) {
            resolvedAccountId = resolveAccountId(accountNumber);
        }
        
        // Ownership check if account provided
        if (resolvedAccountId != null) {
            ownershipService.requireAccountOwnership(resolvedAccountId, authentication);
        } else if (!SecurityUtil.isAdmin(authentication)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Account ID or account number is required");
        }
        
        Sort sortObj = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        TransactionSearchDto searchDto = TransactionSearchDto.builder()
                .accountId(resolvedAccountId)
                .statuses(statuses)
                .types(types)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .startDate(startDate)
                .endDate(endDate)
                .description(description)
                .build();
        
        Page<TransactionResponseDto> transactions = transactionService.searchTransactions(searchDto, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get transaction statistics for account
     * GET /api/transactions/account/{accountIdOrNumber}/stats
     */
    @GetMapping("/account/{accountIdOrNumber}/stats")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionStatsDto> getTransactionStats(
            @PathVariable String accountIdOrNumber,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        ownershipService.requireAccountOwnership(accountId, authentication);
        
        TransactionStatsDto stats = transactionService.getTransactionStats(accountId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all transactions (Admin only)
     * GET /api/transactions
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionResponseDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        Sort sortObj = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        Page<TransactionResponseDto> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    // ========== HELPER METHODS ==========

    /**
     * Resolve account ID from either UUID string or account number
     */
    private UUID resolveAccountId(String accountIdOrNumber) {
        // Try to parse as UUID first
        try {
            return UUID.fromString(accountIdOrNumber);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try to find by account number
            Account account = accountRepository.findByAccountNumber(accountIdOrNumber)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account not found: " + accountIdOrNumber));
            return account.getId();
        }
    }

    /**
     * Resolve account ID from either accountId (UUID) or accountNumber
     * ✅ FIX: Overloaded method for DepositWithAccountRequest and WithdrawWithAccountRequest
     */
    private UUID resolveAccountId(UUID accountId, String accountNumber) {
        if (accountId != null) {
            return accountId;
        }
        if (accountNumber != null && !accountNumber.isBlank()) {
            Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Account not found: " + accountNumber));
            return account.getId();
        }
        throw new IllegalArgumentException("Either accountId or accountNumber is required");
    }

    private Sort createSort(String[] sort) {
        if (sort.length >= 2) {
            return Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
