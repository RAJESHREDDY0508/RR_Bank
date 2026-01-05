package com.rrbank.account.controller;

import com.rrbank.account.dto.AccountDTOs.*;
import com.rrbank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
