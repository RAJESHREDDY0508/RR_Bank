package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.security.CustomUserDetails;
import com.RRBank.banking.service.AccountRequestService;
import com.RRBank.banking.service.AccountService;
import com.RRBank.banking.service.TransactionService;
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

    /**
     * Create new bank account
     * POST /api/accounts
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> createAccount(
            @Valid @RequestBody CreateAccountDto dto) {
        log.info("REST request to create account for customerId: {}", dto.getCustomerId());
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
        String userId = extractUserIdString(authentication);
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
        String userId = extractUserIdString(authentication);
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
        String userId = extractUserIdString(authentication);
        log.info("REST request to cancel account request {} for user: {}", requestId, userId);
        
        AccountRequestResponse response = accountRequestService.cancelRequest(requestId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deposit money to account
     * POST /api/accounts/{accountId}/deposit
     */
    @PostMapping("/{accountId}/deposit")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> deposit(
            @PathVariable UUID accountId,
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {
        log.info("REST request to deposit {} to accountId: {}", request.getAmount(), accountId);
        
        UUID initiatedBy = extractUserId(authentication);
        
        TransactionResponseDto response = transactionService.deposit(
                accountId,
                request.getAmount(),
                request.getDescription(),
                null,
                initiatedBy
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw money from account
     * POST /api/accounts/{accountId}/withdraw
     */
    @PostMapping("/{accountId}/withdraw")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<TransactionResponseDto> withdraw(
            @PathVariable UUID accountId,
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication) {
        log.info("REST request to withdraw {} from accountId: {}", request.getAmount(), accountId);
        
        UUID initiatedBy = extractUserId(authentication);
        
        TransactionResponseDto response = transactionService.withdraw(
                accountId,
                request.getAmount(),
                request.getDescription(),
                null,
                initiatedBy
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get account by ID
     * GET /api/accounts/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> getAccountById(@PathVariable UUID id) {
        log.info("REST request to get account by ID: {}", id);
        AccountResponseDto response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get account by account number
     * GET /api/accounts/number/{accountNumber}
     */
    @GetMapping("/number/{accountNumber}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> getAccountByAccountNumber(@PathVariable String accountNumber) {
        log.info("REST request to get account by number: {}", accountNumber);
        AccountResponseDto response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all accounts for a customer
     * GET /api/accounts/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getAccountsByCustomerId(@PathVariable UUID customerId) {
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
    public ResponseEntity<List<AccountResponseDto>> getActiveAccountsByCustomer(@PathVariable UUID customerId) {
        log.info("REST request to get active accounts for customerId: {}", customerId);
        List<AccountResponseDto> accounts = accountService.getActiveAccountsByCustomer(customerId);
        return ResponseEntity.ok(accounts);
    }

    /**
     * Get account balance (with Redis caching)
     * GET /api/accounts/{id}/balance
     */
    @GetMapping("/{id}/balance")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<BalanceResponseDto> getAccountBalance(@PathVariable UUID id) {
        log.info("REST request to get balance for accountId: {}", id);
        BalanceResponseDto response = accountService.getAccountBalance(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get total balance for customer
     * GET /api/accounts/customer/{customerId}/total-balance
     */
    @GetMapping("/customer/{customerId}/total-balance")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getTotalBalanceForCustomer(@PathVariable UUID customerId) {
        log.info("REST request to get total balance for customerId: {}", customerId);
        BigDecimal totalBalance = accountService.getTotalBalanceForCustomer(customerId);
        return ResponseEntity.ok(totalBalance);
    }

    /**
     * Update account status
     * PUT /api/accounts/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> updateAccountStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusDto dto,
            Authentication authentication) {
        log.info("REST request to update status for accountId: {} to {}", id, dto.getStatus());
        String changedBy = authentication != null ? authentication.getName() : "SYSTEM";
        AccountResponseDto response = accountService.updateAccountStatus(id, dto, changedBy);
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
     * Get all accounts for admin view (includes customer accounts)
     * GET /api/accounts/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountResponseDto>> getAllAccountsForAdmin() {
        log.info("REST request to get all accounts for admin view");
        List<AccountResponseDto> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Delete account (Admin only - only if balance is zero and account is closed)
     * DELETE /api/accounts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        log.info("REST request to delete account with ID: {}", id);
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ========== HELPER METHODS ==========

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse user ID from authentication: {}", authentication.getName());
            return null;
        }
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
