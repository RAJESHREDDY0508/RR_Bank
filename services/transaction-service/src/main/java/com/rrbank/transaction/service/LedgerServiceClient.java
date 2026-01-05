package com.rrbank.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LedgerServiceClient {

    private final WebClient webClient;

    public LedgerServiceClient(@Value("${services.ledger-url:http://localhost:8085}") String ledgerUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(ledgerUrl)
                .build();
    }

    public void credit(UUID accountId, UUID transactionId, BigDecimal amount, String description) {
        log.info("Calling Ledger Service: credit {} to account {}", amount, accountId);
        
        Map<String, Object> request = new HashMap<>();
        request.put("accountId", accountId.toString());
        request.put("transactionId", transactionId.toString());
        request.put("entryType", "CREDIT");
        request.put("amount", amount);
        request.put("description", description);

        webClient.post()
                .uri("/api/ledger/credit")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        log.info("Ledger credit completed for account: {}", accountId);
    }

    public void debit(UUID accountId, UUID transactionId, BigDecimal amount, String description) {
        log.info("Calling Ledger Service: debit {} from account {}", amount, accountId);
        
        Map<String, Object> request = new HashMap<>();
        request.put("accountId", accountId.toString());
        request.put("transactionId", transactionId.toString());
        request.put("entryType", "DEBIT");
        request.put("amount", amount);
        request.put("description", description);

        webClient.post()
                .uri("/api/ledger/debit")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        log.info("Ledger debit completed for account: {}", accountId);
    }

    public BigDecimal getBalance(UUID accountId) {
        log.info("Calling Ledger Service: get balance for account {}", accountId);
        
        Map response = webClient.get()
                .uri("/api/ledger/balance/{accountId}", accountId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        return new BigDecimal(response.get("balance").toString());
    }
}
