package com.rrbank.ledger.controller;

import com.rrbank.ledger.dto.LedgerDTOs.*;
import com.rrbank.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Slf4j
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/credit")
    public ResponseEntity<LedgerEntryResponse> credit(@Valid @RequestBody CreateEntryRequest request) {
        log.info("POST credit for account: {}", request.getAccountId());
        return ResponseEntity.ok(ledgerService.credit(request));
    }

    @PostMapping("/debit")
    public ResponseEntity<LedgerEntryResponse> debit(@Valid @RequestBody CreateEntryRequest request) {
        log.info("POST debit for account: {}", request.getAccountId());
        return ResponseEntity.ok(ledgerService.debit(request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("POST transfer from {} to {}", request.getFromAccountId(), request.getToAccountId());
        ledgerService.transfer(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/balance/{accountId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId) {
        log.info("GET balance for account: {}", accountId);
        return ResponseEntity.ok(ledgerService.getBalance(accountId));
    }

    @GetMapping("/entries/{accountId}")
    public ResponseEntity<Page<LedgerEntryResponse>> getEntries(
            @PathVariable UUID accountId, Pageable pageable) {
        log.info("GET entries for account: {}", accountId);
        return ResponseEntity.ok(ledgerService.getEntries(accountId, pageable));
    }

    @PostMapping("/rebuild-cache/{accountId}")
    public ResponseEntity<Void> rebuildCache(@PathVariable UUID accountId) {
        log.info("POST rebuild cache for account: {}", accountId);
        ledgerService.rebuildBalanceCache(accountId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ledger Service is healthy");
    }
}
