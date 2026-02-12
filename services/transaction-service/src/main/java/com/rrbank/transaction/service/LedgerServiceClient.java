package com.rrbank.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LedgerServiceClient {

    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public LedgerServiceClient(@Value("${services.ledger-url:http://localhost:8085}") String ledgerUrl) {
        log.info("Initializing LedgerServiceClient with URL: {}", ledgerUrl);
        this.webClient = WebClient.builder()
                .baseUrl(ledgerUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void credit(UUID accountId, UUID transactionId, BigDecimal amount, String description) {
        log.info("Calling Ledger Service: credit {} to account {}", amount, accountId);
        
        // Create properly typed request object
        LedgerRequest request = new LedgerRequest(accountId, transactionId, "CREDIT", amount, description);
        
        try {
            Map response = webClient.post()
                    .uri("/api/ledger/credit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
            
            log.info("Ledger credit completed for account: {}, response: {}", accountId, response);
        } catch (WebClientResponseException e) {
            log.error("Ledger credit failed - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Ledger credit failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ledger credit failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ledger credit failed: " + e.getMessage(), e);
        }
    }

    public void debit(UUID accountId, UUID transactionId, BigDecimal amount, String description) {
        log.info("Calling Ledger Service: debit {} from account {}", amount, accountId);
        
        // Create properly typed request object
        LedgerRequest request = new LedgerRequest(accountId, transactionId, "DEBIT", amount, description);
        
        try {
            Map response = webClient.post()
                    .uri("/api/ledger/debit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
            
            log.info("Ledger debit completed for account: {}, response: {}", accountId, response);
        } catch (WebClientResponseException e) {
            log.error("Ledger debit failed - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Ledger debit failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ledger debit failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ledger debit failed: " + e.getMessage(), e);
        }
    }

    public BigDecimal getBalance(UUID accountId) {
        log.info("Calling Ledger Service: get balance for account {}", accountId);
        
        try {
            Map response = webClient.get()
                    .uri("/api/ledger/balance/{accountId}", accountId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
            
            if (response != null && response.containsKey("balance")) {
                BigDecimal balance = new BigDecimal(response.get("balance").toString());
                log.info("Got balance for account {}: {}", accountId, balance);
                return balance;
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to get balance for account {}: {}", accountId, e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    // Inner class for proper JSON serialization with UUID fields
    private static class LedgerRequest {
        public UUID accountId;
        public UUID transactionId;
        public String entryType;
        public BigDecimal amount;
        public String description;
        
        public LedgerRequest(UUID accountId, UUID transactionId, String entryType, BigDecimal amount, String description) {
            this.accountId = accountId;
            this.transactionId = transactionId;
            this.entryType = entryType;
            this.amount = amount;
            this.description = description;
        }
    }
}
