package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Account;
import com.RRBank.banking.exception.ResourceNotFoundException;
import com.RRBank.banking.repository.AccountRepository;
import com.RRBank.banking.service.AccountRequestService;
import com.RRBank.banking.service.AccountService;
import com.RRBank.banking.service.OwnershipService;
import com.RRBank.banking.service.TransactionService;
import com.RRBank.banking.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Account Controller
 * REST API endpoints for account management
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AccountRequestService accountRequestService;
    private final OwnershipService ownershipService;
    private final AccountRepository accountRepository;

    /**
     * Create new bank account
     * POST /api/accounts
     * 
     * ✅ FIX: customerId is now optional - will be auto-detected from authenticated user
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> createAccount(
            @Valid @RequestBody CreateAccountDto dto,
            Authentication authentication) {
        
        // If customerId not provided, resolve it from authenticated user
        if (dto.getCustomerId() == null) {
            UUID userId = SecurityUtil.requireUserId(authentication);
            UUID customerId = accountService.resolveCustomerIdFromUserId(userId);
            dto.setCustomerId(customerId);
            log.info("REST request to create account - customerId resolved from user: {}", customerId);
        } else {
            log.info("REST request to create account for customerId: {}", dto.getCustomerId());
        }
        
        AccountResponseDto response = accountService.createAccount(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get my accounts (for authenticated user)
     * GET /api/accounts/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getMyAccounts() {
        log.info("REST request to get accounts for authenticated user");
        List<AccountResponseDto> accounts = accountService.getMyAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get my account requests (for authenticated user)
     * GET /api/accounts/requests
     */
    @GetMapping("/requests")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountRequestResponse>> getMyAccountRequests(Authentication authentication) {
        String userId = SecurityUtil.requireUserIdString(authentication);
        log.info("REST request to get account requests for user: {}", userId);
        
        List<AccountRequestResponse> requests = accountRequestService.getUserRequests(userId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Submit account opening request
     * POST /api/accounts/requests
     */
    @PostMapping("/requests")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountRequestResponse> submitAccountRequest(
            @Valid @RequestBody AccountOpenRequest request,
            Authentication authentication) {
        String userId = SecurityUtil.requireUserIdString(authentication);
        log.info("REST request to submit account request for user: {}", userId);
        
        AccountRequestResponse response = accountRequestService.submitRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel account request
     * DELETE /api/accounts/requests/{requestId}
     */
    @DeleteMapping("/requests/{requestId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountRequestResponse> cancelAccountRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        String userId = SecurityUtil.requireUserIdString(authentication);
        log.info("REST request to cancel account request {} for user: {}", requestId, userId);
        
        AccountRequestResponse response = accountRequestService.cancelRequest(requestId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deposit money to account
     * POST /api/accounts/{accountIdOrNumber}/deposit
     * 
     * Accepts either UUID or account number
     */
    @PostMapping("/{accountIdOrNumber}/deposit")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> deposit(
            @PathVariable String accountIdOrNumber,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to deposit {} to account: {}", request.getAmount(), accountId);
        
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
     * POST /api/accounts/{accountIdOrNumber}/withdraw
     * 
     * Accepts either UUID or account number
     */
    @PostMapping("/{accountIdOrNumber}/withdraw")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> withdraw(
            @PathVariable String accountIdOrNumber,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to withdraw {} from account: {}", request.getAmount(), accountId);
        
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

    /**
     * Get account by ID or account number
     * GET /api/accounts/{accountIdOrNumber}
     */
    @GetMapping("/{accountIdOrNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> getAccount(
            @PathVariable String accountIdOrNumber,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        // Ownership check
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to get account: {}", accountId);
        
        AccountResponseDto response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all accounts for a customer
     * GET /api/accounts/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByCustomerId(
            @PathVariable UUID customerId,
            Authentication authentication) {
        
        // Ownership check
        ownershipService.requireCustomerOwnership(customerId, authentication);
        log.info("REST request to get accounts for customerId: {}", customerId);
        
        List<AccountResponseDto> accounts = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get active accounts for customer
     * GET /api/accounts/customer/{customerId}/active
     */
    @GetMapping("/customer/{customerId}/active")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getActiveAccountsByCustomer(
            @PathVariable UUID customerId,
            Authentication authentication) {
        
        ownershipService.requireCustomerOwnership(customerId, authentication);
        log.info("REST request to get active accounts for customerId: {}", customerId);
        
        List<AccountResponseDto> accounts = accountService.getActiveAccountsByCustomer(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get account balance
     * GET /api/accounts/{accountIdOrNumber}/balance
     */
    @GetMapping("/{accountIdOrNumber}/balance")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<BalanceResponseDto> getAccountBalance(
            @PathVariable String accountIdOrNumber,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(accountIdOrNumber);
        
        ownershipService.requireAccountOwnership(accountId, authentication);
        log.info("REST request to get balance for account: {}", accountId);
        
        BalanceResponseDto response = accountService.getAccountBalance(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get total balance for customer
     * GET /api/accounts/customer/{customerId}/total-balance
     */
    @GetMapping("/customer/{customerId}/total-balance")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalBalanceForCustomer(
            @PathVariable UUID customerId,
            Authentication authentication) {
        
        ownershipService.requireCustomerOwnership(customerId, authentication);
        log.info("REST request to get total balance for customerId: {}", customerId);
        
        BigDecimal totalBalance = accountService.getTotalBalanceForCustomer(customerId);
        return ResponseEntity.ok(totalBalance);
    }

    /**
     * Update account status (Admin only)
     * PUT /api/accounts/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> updateAccountStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountStatusDto dto,
            Authentication authentication) {
        
        UUID accountId = resolveAccountId(id);
        log.info("REST request to update status for account: {} to {}", accountId, dto.getStatus());
        
        String changedBy = SecurityUtil.getUsername(authentication);
        AccountResponseDto response = accountService.updateAccountStatus(accountId, dto, changedBy);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all accounts (Admin only)
     * GET /api/accounts
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getAllAccounts() {
        log.info("REST request to get all accounts");
        List<AccountResponseDto> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get accounts by status (Admin only)
     * GET /api/accounts/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByStatus(@PathVariable String status) {
        log.info("REST request to get accounts by status: {}", status);
        List<AccountResponseDto> accounts = accountService.getAccountsByStatus(status);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Search accounts (Admin only)
     * GET /api/accounts/search?query=...
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> searchAccounts(@RequestParam String query) {
        log.info("REST request to search accounts with query: {}", query);
        List<AccountResponseDto> accounts = accountService.searchAccounts(query);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Delete account (Admin only)
     * DELETE /api/accounts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable String id) {
        UUID accountId = resolveAccountId(id);
        log.info("REST request to delete account: {}", accountId);
        accountService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
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
}
