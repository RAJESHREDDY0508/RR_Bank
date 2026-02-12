package com.rrbank.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CustomerServiceClient {

    private final WebClient webClient;
    private final String customerUrl;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    public CustomerServiceClient(@Value("${services.customer-url:http://localhost:8082}") String customerUrl) {
        this.customerUrl = customerUrl;
        log.info("Initializing CustomerServiceClient with URL: {}", customerUrl);
        this.webClient = WebClient.builder()
                .baseUrl(customerUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Create a customer record when a user registers
     */
    public void createCustomer(UUID userId, String email, String firstName, String lastName, String phoneNumber) {
        log.info("=== CREATING CUSTOMER RECORD ===");
        log.info("URL: {}/api/customers", customerUrl);
        log.info("Data: userId={}, email={}, firstName={}, lastName={}", userId, email, firstName, lastName);
        
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("userId", userId.toString());
        customerData.put("email", email);
        customerData.put("firstName", firstName != null ? firstName : "User");
        customerData.put("lastName", lastName != null ? lastName : "Unknown");
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            customerData.put("phoneNumber", phoneNumber);
        }
        customerData.put("kycVerified", false);
        
        log.info("Request body: {}", customerData);
        
        try {
            String response = webClient.post()
                    .uri("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(customerData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            
            log.info("=== CUSTOMER CREATED SUCCESSFULLY ===");
            log.info("Response: {}", response);
        } catch (WebClientResponseException e) {
            log.error("=== CUSTOMER CREATION FAILED ===");
            log.error("Status: {}", e.getStatusCode());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("=== CUSTOMER CREATION FAILED ===");
            log.error("Error: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if a customer record exists for a user
     */
    public boolean customerExists(UUID userId) {
        log.info("Checking if customer exists for userId: {}", userId);
        try {
            webClient.get()
                    .uri("/api/customers/user/{userId}", userId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            log.info("Customer exists for userId: {}", userId);
            return true;
        } catch (WebClientResponseException.NotFound e) {
            log.info("Customer does not exist for userId: {}", userId);
            return false;
        } catch (Exception e) {
            log.warn("Error checking customer existence: {}", e.getMessage());
            return false;
        }
    }
}
