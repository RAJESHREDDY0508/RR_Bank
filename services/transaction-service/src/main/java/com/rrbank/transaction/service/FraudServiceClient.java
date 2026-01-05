package com.rrbank.transaction.service;

import com.rrbank.transaction.dto.TransactionDTOs.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
public class FraudServiceClient {

    private final WebClient webClient;

    public FraudServiceClient(@Value("${services.fraud-url:http://localhost:8087}") String fraudUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(fraudUrl)
                .build();
    }

    public FraudCheckResponse checkTransaction(FraudCheckRequest request) {
        log.info("Calling Fraud Service: check transaction for account {}", request.getAccountId());
        
        try {
            FraudCheckResponse response = webClient.post()
                    .uri("/api/fraud/check")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FraudCheckResponse.class)
                    .block();
            
            log.info("Fraud check completed: decision={}", response.getDecision());
            return response;
        } catch (WebClientResponseException e) {
            log.error("Fraud service error: {}", e.getMessage());
            return FraudCheckResponse.builder()
                    .decision("APPROVE")
                    .reason("Fraud service unavailable - default approve")
                    .riskScore(0)
                    .build();
        }
    }
}
