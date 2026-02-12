package com.rrbank.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceClientService {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.customer-url:http://localhost:8082}")
    private String customerServiceUrl;

    @Value("${services.account-url:http://localhost:8083}")
    private String accountServiceUrl;

    @Value("${services.transaction-url:http://localhost:8084}")
    private String transactionServiceUrl;

    @Value("${services.ledger-url:http://localhost:8085}")
    private String ledgerServiceUrl;

    @Value("${services.fraud-url:http://localhost:8087}")
    private String fraudServiceUrl;

    @Value("${services.audit-url:http://localhost:8088}")
    private String auditServiceUrl;

    @Value("${services.auth-url:http://localhost:8081}")
    private String authServiceUrl;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(500);

    // Customer Service Methods
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomers(int page, int size, String search) {
        String url = customerServiceUrl + "/api/customers?page=" + page + "&size=" + size;
        if (search != null && !search.isEmpty()) {
            url += "&search=" + search;
        }
        return fetchAsMap(url);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomer(String customerId) {
        return fetchAsMap(customerServiceUrl + "/api/customers/" + customerId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomerStats() {
        return fetchAsMap(customerServiceUrl + "/api/customers/stats");
    }

    // Account Service Methods
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccounts(int page, int size, String status, String accountType, String search) {
        StringBuilder url = new StringBuilder(accountServiceUrl + "/api/accounts?page=" + page + "&size=" + size);
        if (status != null && !status.isEmpty()) {
            url.append("&status=").append(status);
        }
        if (accountType != null && !accountType.isEmpty()) {
            url.append("&accountType=").append(accountType);
        }
        if (search != null && !search.isEmpty()) {
            url.append("&search=").append(search);
        }
        return fetchAsMap(url.toString());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(String accountId) {
        return fetchAsMap(accountServiceUrl + "/api/accounts/" + accountId);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAccountsByUser(String userId) {
        return fetchAsList(accountServiceUrl + "/api/accounts/user/" + userId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccountStats() {
        return fetchAsMap(accountServiceUrl + "/api/accounts/stats");
    }

    public Map<String, Object> updateAccountStatus(String accountId, String status) {
        try {
            return webClientBuilder.build()
                    .patch()
                    .uri(accountServiceUrl + "/api/accounts/" + accountId + "/status?status=" + status)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .block();
        } catch (Exception e) {
            log.error("Failed to update account status after retries: {}", e.getMessage());
            return null;
        }
    }

    // Transaction Service Methods
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTransactions(int page, int size, String status, String type) {
        StringBuilder url = new StringBuilder(transactionServiceUrl + "/api/transactions?page=" + page + "&size=" + size);
        if (status != null && !status.isEmpty()) {
            url.append("&status=").append(status);
        }
        if (type != null && !type.isEmpty()) {
            url.append("&type=").append(type);
        }
        return fetchAsMap(url.toString());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTransaction(String transactionId) {
        return fetchAsMap(transactionServiceUrl + "/api/transactions/" + transactionId);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTransactionsByAccount(String accountId, int page, int size) {
        return fetchAsMap(transactionServiceUrl + "/api/transactions/account/" + accountId + "?page=" + page + "&size=" + size);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTransactionStats() {
        return fetchAsMap(transactionServiceUrl + "/api/transactions/stats");
    }

    // Ledger Service Methods
    @SuppressWarnings("unchecked")
    public Map<String, Object> getLedgerStats() {
        return fetchAsMap(ledgerServiceUrl + "/api/ledger/stats");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccountBalance(String accountId) {
        return fetchAsMap(ledgerServiceUrl + "/api/ledger/balance/" + accountId);
    }

    // Fraud Service Methods
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFraudStats() {
        return fetchAsMap(fraudServiceUrl + "/api/fraud/stats");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserFraudLimits(String userId) {
        return fetchAsMap(fraudServiceUrl + "/api/fraud/limits/" + userId);
    }

    // KYC Management Methods with enhanced retry logic
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPendingKycCustomers(int page, int size) {
        String url = customerServiceUrl + "/api/customers/kyc/pending?page=" + page + "&size=" + size;
        return fetchAsMapWithRetry(url, "getPendingKycCustomers");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getKycStats() {
        String url = customerServiceUrl + "/api/customers/kyc/stats";
        return fetchAsMapWithRetry(url, "getKycStats");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getCustomersByKycStatus(String status, int page, int size) {
        String url = customerServiceUrl + "/api/customers/kyc/status/" + status + "?page=" + page + "&size=" + size;
        return fetchAsMapWithRetry(url, "getCustomersByKycStatus");
    }

    public Map<String, Object> approveKyc(String customerId) {
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(customerServiceUrl + "/api/customers/" + customerId + "/kyc/approve")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .block();
        } catch (Exception e) {
            log.error("Failed to approve KYC for customer {} after retries: {}", customerId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> approveKycByUserId(String userId) {
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(customerServiceUrl + "/api/customers/user/" + userId + "/kyc/approve")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .block();
        } catch (Exception e) {
            log.error("Failed to approve KYC for user {} after retries: {}", userId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> rejectKyc(String customerId, String reason) {
        try {
            Map<String, String> body = reason != null ? Map.of("reason", reason) : Map.of();
            return webClientBuilder.build()
                    .post()
                    .uri(customerServiceUrl + "/api/customers/" + customerId + "/kyc/reject")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .block();
        } catch (Exception e) {
            log.error("Failed to reject KYC for customer {} after retries: {}", customerId, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> rejectKycByUserId(String userId, String reason) {
        try {
            Map<String, String> body = reason != null ? Map.of("reason", reason) : Map.of();
            return webClientBuilder.build()
                    .post()
                    .uri(customerServiceUrl + "/api/customers/user/" + userId + "/kyc/reject")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .block();
        } catch (Exception e) {
            log.error("Failed to reject KYC for user {} after retries: {}", userId, e.getMessage());
            return null;
        }
    }

    // Enhanced fetch method with retry logic for critical KYC endpoints
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAsMapWithRetry(String url, String operationName) {
        try {
            log.debug("[{}] Fetching from: {}", operationName, url);
            
            Map<String, Object> result = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException)
                            .doBeforeRetry(retrySignal -> 
                                log.warn("[{}] Retrying request to {} (attempt {})", 
                                    operationName, url, retrySignal.totalRetries() + 1)))
                    .onErrorResume(e -> {
                        log.error("[{}] Failed to fetch from {} after {} retries: {}", 
                            operationName, url, MAX_RETRY_ATTEMPTS, e.getMessage());
                        return Mono.just(Collections.emptyMap());
                    })
                    .block();
            
            if (result != null && !result.isEmpty()) {
                log.debug("[{}] Successfully fetched from {}", operationName, url);
                return result;
            }
            
            log.warn("[{}] Received empty response from {}", operationName, url);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("[{}] Unexpected error fetching from {}: {}", operationName, url, e.getMessage());
            return Collections.emptyMap();
        }
    }

    // Generic fetch methods with retry
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchAsMap(String url) {
        try {
            log.debug("Fetching from: {}", url);
            Map<String, Object> result = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .onErrorResume(e -> {
                        log.warn("Failed to fetch from {}: {}", url, e.getMessage());
                        return Mono.just(Collections.emptyMap());
                    })
                    .block();
            
            if (result != null && !result.isEmpty()) {
                log.debug("Successfully fetched from {}", url);
            }
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch from {}: {}", url, e.getMessage());
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchAsList(String url) {
        try {
            log.debug("Fetching list from: {}", url);
            List<Map<String, Object>> result = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(TIMEOUT)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
                            .filter(this::isRetryableException))
                    .onErrorResume(e -> {
                        log.warn("Failed to fetch list from {}: {}", url, e.getMessage());
                        return Mono.just(Collections.emptyList());
                    })
                    .block();
            
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch list from {}: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Determine if an exception is retryable
     */
    private boolean isRetryableException(Throwable throwable) {
        // Retry on connection errors, timeouts, and 5xx server errors
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            int statusCode = ex.getStatusCode().value();
            return statusCode >= 500 && statusCode < 600;
        }
        // Retry on connection timeouts and connection refused
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException ||
               throwable.getMessage().contains("Connection refused") ||
               throwable.getMessage().contains("Connection reset");
    }
}
