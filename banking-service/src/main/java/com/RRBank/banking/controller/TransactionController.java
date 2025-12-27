package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.security.CustomUserDetails;
import com.RRBank.banking.service.TransactionLimitService;
import com.RRBank.banking.service.TransactionService;
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

    /**
     * Transfer money between accounts
     * POST /api/transactions/transfer
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> transfer(
            @Valid @RequestBody TransferRequestDto request,
            Authentication authentication) {
        log.info("REST request to transfer from {} to {} amount: {}", 
                request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        
        // Get user ID from authentication
        UUID userId = extractUserIdFromAuthentication(authentication);
        
        TransactionResponseDto response = transactionService.transfer(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get transaction limits for authenticated user
     * GET /api/transactions/limits
     */
    @GetMapping("/limits")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<TransactionLimitResponse>> getMyLimits(Authentication authentication) {
        String userId = extractUserIdString(authentication);
        log.info("REST request to get transaction limits for user: {}", userId);
        
        List<TransactionLimitResponse> limits = transactionLimitService.getLimitsForUser(userId);
        return ResponseEntity.ok(limits);
    }

    /**
     * Get transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable UUID id) {
        log.info("REST request to get transaction by ID: {}", id);
        TransactionResponseDto response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction by reference
     * GET /api/transactions/reference/{reference}
     */
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> getTransactionByReference(@PathVariable String reference) {
        log.info("REST request to get transaction by reference: {}", reference);
        TransactionResponseDto response = transactionService.getTransactionByReference(reference);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all transactions for an account
     * GET /api/transactions/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionsByAccountId(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        log.info("REST request to get transactions for accountId: {} - page: {}, size: {}", 
                accountId, page, size);
        
        Sort sortObj = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        Page<TransactionResponseDto> transactions = transactionService.getTransactionsByAccountId(accountId, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get recent transactions for account
     * GET /api/transactions/account/{accountId}/recent?limit=10
     */
    @GetMapping("/account/{accountId}/recent")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<TransactionResponseDto>> getRecentTransactions(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST request to get {} recent transactions for accountId: {}", limit, accountId);
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
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        log.info("REST request to search transactions with filters");
        
        Sort sortObj = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        TransactionSearchDto searchDto = TransactionSearchDto.builder()
                .accountId(accountId)
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
     * GET /api/transactions/account/{accountId}/stats
     */
    @GetMapping("/account/{accountId}/stats")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionStatsDto> getTransactionStats(@PathVariable UUID accountId) {
        log.info("REST request to get transaction statistics for accountId: {}", accountId);
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
        
        log.info("REST request to get all transactions - page: {}, size: {}", page, size);
        
        Sort sortObj = createSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        
        Page<TransactionResponseDto> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions);
    }

    // ========== HELPER METHODS ==========

    private Sort createSort(String[] sort) {
        if (sort.length >= 2) {
            return Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse userId from authentication: {}", authentication.getName());
                return null;
            }
        }
        return null;
    }

    private String extractUserIdString(Authentication authentication) {
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUserId();
            }
            return authentication.getName();
        }
        return null;
    }
}
