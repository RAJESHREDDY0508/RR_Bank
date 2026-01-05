package com.rrbank.account.service;

import com.rrbank.account.dto.AccountDTOs.*;
import com.rrbank.account.entity.Account;
import com.rrbank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.ledger-url:http://localhost:8085}")
    private String ledgerServiceUrl;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for user: {}", request.getUserId());

        Account account = Account.builder()
                .userId(UUID.fromString(request.getUserId()))
                .customerId(request.getCustomerId() != null ? UUID.fromString(request.getCustomerId()) : null)
                .accountType(Account.AccountType.valueOf(request.getAccountType().toUpperCase()))
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .overdraftLimit(request.getOverdraftLimit() != null ? request.getOverdraftLimit() : BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        account = accountRepository.save(account);
        log.info("Account created: {}", account.getId());

        return toResponse(account, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        BigDecimal balance = getBalanceFromLedger(accountId);
        return toResponse(account, balance);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        
        BigDecimal balance = getBalanceFromLedger(account.getId());
        return toResponse(account, balance);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByUser(UUID userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(acc -> toResponse(acc, getBalanceFromLedger(acc.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        BigDecimal balance = getBalanceFromLedger(accountId);
        BigDecimal available = balance.add(account.getOverdraftLimit());

        return BalanceResponse.builder()
                .accountId(accountId.toString())
                .accountNumber(account.getAccountNumber())
                .balance(balance)
                .availableBalance(available)
                .currency(account.getCurrency())
                .asOf(LocalDateTime.now())
                .build();
    }

    @Transactional
    public AccountResponse updateStatus(UUID accountId, String status) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        account.setStatus(Account.AccountStatus.valueOf(status.toUpperCase()));
        if (account.getStatus() == Account.AccountStatus.CLOSED) {
            account.setClosedAt(LocalDateTime.now());
        }
        
        account = accountRepository.save(account);
        return toResponse(account, getBalanceFromLedger(accountId));
    }

    private BigDecimal getBalanceFromLedger(UUID accountId) {
        try {
            WebClient client = webClientBuilder.baseUrl(ledgerServiceUrl).build();
            Map response = client.get()
                    .uri("/api/ledger/balance/{accountId}", accountId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            return response != null && response.get("balance") != null 
                    ? new BigDecimal(response.get("balance").toString())
                    : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get balance from ledger service: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private AccountResponse toResponse(Account account, BigDecimal balance) {
        return AccountResponse.builder()
                .id(account.getId().toString())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId().toString())
                .customerId(account.getCustomerId() != null ? account.getCustomerId().toString() : null)
                .accountType(account.getAccountType().name())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .balance(balance)
                .availableBalance(balance.add(account.getOverdraftLimit()))
                .overdraftLimit(account.getOverdraftLimit())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
