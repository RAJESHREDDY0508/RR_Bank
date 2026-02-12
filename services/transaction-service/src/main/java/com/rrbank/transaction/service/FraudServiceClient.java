package com.rrbank.transaction.service;

import com.rrbank.transaction.dto.TransactionDTOs.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
@Slf4j
public class FraudServiceClient {

    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public FraudServiceClient(@Value("${services.fraud-url:http://localhost:8087}") String fraudUrl) {
        log.info("Initializing FraudServiceClient with URL: {}", fraudUrl);
        this.webClient = WebClient.builder()
                .baseUrl(fraudUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public FraudCheckResponse checkTransaction(FraudCheckRequest request) {
        log.info("Calling Fraud Service: check transaction for account {}, type {}, amount {}", 
                request.getAccountId(), request.getTransactionType(), request.getAmount());
        
        try {
            FraudCheckResponse response = webClient.post()
                    .uri("/api/fraud/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FraudCheckResponse.class)
                    .timeout(TIMEOUT)
                    .block();
            
            log.info("Fraud check completed: decision={}, reason={}", 
                    response != null ? response.getDecision() : "null",
                    response != null ? response.getReason() : "null");
            return response;
        } catch (WebClientResponseException e) {
            log.error("Fraud service error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Default to approve if fraud service is unavailable (fail-open for deposits)
            return FraudCheckResponse.builder()
                    .decision("APPROVE")
                    .reason("Fraud service error - default approve: " + e.getMessage())
                    .riskScore(0)
                    .build();
        } catch (Exception e) {
            log.error("Fraud service unavailable: {}", e.getMessage());
            // Default to approve if fraud service is unavailable
            return FraudCheckResponse.builder()
                    .decision("APPROVE")
                    .reason("Fraud service unavailable - default approve")
                    .riskScore(0)
                    .build();
        }
    }
}
