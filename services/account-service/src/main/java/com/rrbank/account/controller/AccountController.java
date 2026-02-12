package com.rrbank.account.controller;

import com.rrbank.account.dto.AccountDTOs.*;
import com.rrbank.account.entity.Account;
import com.rrbank.account.repository.AccountRepository;
import com.rrbank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final AccountRepository accountRepository;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("POST create account");
        
        if (request.getUserId() == null && userId != null) {
            request.setUserId(userId);
        }
        
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("GET all accounts - page: {}, size: {}, status: {}, type: {}", page, size, status, accountType);
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Account> accounts;
        if (status != null && !status.isEmpty()) {
            accounts = accountRepository.findByStatus(status, pageable);
        } else if (accountType != null && !accountType.isEmpty()) {
            accounts = accountRepository.findByAccountType(accountType, pageable);
        } else if (search != null && !search.isEmpty()) {
            accounts = accountRepository.searchAccounts(search, pageable);
        } else {
            accounts = accountRepository.findAll(pageable);
        }
        
        Page<AccountResponse> response = accounts.map(this::toAccountResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) {
        log.info("GET account: {}", accountId);
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(@PathVariable String accountNumber) {
        log.info("GET account by number: {}", accountNumber);
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getAccountsByUser(@PathVariable UUID userId) {
        log.info("GET accounts for user: {}", userId);
        return ResponseEntity.ok(accountService.getAccountsByUser(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @RequestHeader("X-User-Id") String userId) {
        log.info("GET my accounts for user: {}", userId);
        return ResponseEntity.ok(accountService.getAccountsByUser(UUID.fromString(userId)));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId) {
        log.info("GET balance for account: {}", accountId);
        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @PatchMapping("/{accountId}/status")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable UUID accountId,
            @RequestParam String status) {
        log.info("PATCH account status: {} -> {}", accountId, status);
        return ResponseEntity.ok(accountService.updateStatus(accountId, status));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Account Service is healthy");
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId().toString())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().name())
                .status(account.getStatus().name())
                .currency(account.getCurrency())
                .userId(account.getUserId() != null ? account.getUserId().toString() : null)
                .customerId(account.getCustomerId() != null ? account.getCustomerId().toString() : null)
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
