package com.rrbank.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CustomerServiceClient {

    private final WebClient webClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public CustomerServiceClient(@Value("${services.customer-url:http://localhost:8082}") String customerUrl) {
        log.info("Initializing CustomerServiceClient with URL: {}", customerUrl);
        this.webClient = WebClient.builder()
                .baseUrl(customerUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Check if user's KYC is approved
     * @param userId The user ID to check
     * @return KYC status response containing kycStatus and rejectionReason
     */
    @SuppressWarnings("unchecked")
    public KycStatusResponse getKycStatus(UUID userId) {
        log.info("Checking KYC status for user: {}", userId);
        
        try {
            Map<String, String> response = webClient.get()
                    .uri("/api/customers/user/{userId}/kyc-status", userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();
            
            if (response != null) {
                String kycStatus = response.get("kycStatus");
                String rejectionReason = response.get("rejectionReason");
                log.info("KYC status for user {}: {}", userId, kycStatus);
                return new KycStatusResponse(kycStatus, rejectionReason);
            }
            
            // If no response, default to PENDING for safety
            log.warn("No KYC status response for user: {}, defaulting to PENDING", userId);
            return new KycStatusResponse("PENDING", null);
            
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Customer not found for user: {}, defaulting to PENDING", userId);
            return new KycStatusResponse("PENDING", null);
        } catch (Exception e) {
            log.error("Failed to get KYC status for user: {} - {}", userId, e.getMessage());
            // For safety, if we can't verify KYC, treat as PENDING
            return new KycStatusResponse("PENDING", null);
        }
    }

    /**
     * Check if user's KYC is approved
     * @param userId The user ID to check
     * @return true if KYC is APPROVED, false otherwise
     */
    public boolean isKycApproved(UUID userId) {
        KycStatusResponse status = getKycStatus(userId);
        return "APPROVED".equals(status.kycStatus());
    }

    /**
     * KYC Status Response record
     */
    public record KycStatusResponse(String kycStatus, String rejectionReason) {
        public boolean isApproved() {
            return "APPROVED".equals(kycStatus);
        }
        
        public boolean isPending() {
            return "PENDING".equals(kycStatus);
        }
        
        public boolean isRejected() {
            return "REJECTED".equals(kycStatus);
        }
    }
}
