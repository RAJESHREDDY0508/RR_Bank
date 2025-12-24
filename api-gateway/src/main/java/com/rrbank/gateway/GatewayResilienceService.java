package com.RRBank.banking.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Gateway Service
 * Demonstrates usage of Resilience4j patterns for fault tolerance
 * Provides circuit breaker, retry, and timeout capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayResilienceService {

    /**
     * Execute request with Circuit Breaker
     * Prevents cascading failures by opening circuit when failure threshold is reached
     */
    @CircuitBreaker(name = "gatewayCircuitBreaker", fallbackMethod = "fallbackCircuitBreaker")
    public <T> T executeWithCircuitBreaker(String serviceName, ServiceCall<T> serviceCall) {
        log.debug("Executing request to {} with circuit breaker", serviceName);
        try {
            return serviceCall.call();
        } catch (Exception e) {
            log.error("Error executing request to {}: {}", serviceName, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute request with Retry
     * Automatically retries failed requests with exponential backoff
     */
    @Retry(name = "gatewayRetry", fallbackMethod = "fallbackRetry")
    public <T> T executeWithRetry(String serviceName, ServiceCall<T> serviceCall) {
        log.debug("Executing request to {} with retry", serviceName);
        try {
            return serviceCall.call();
        } catch (Exception e) {
            log.warn("Request to {} failed, will retry: {}", serviceName, e.getMessage());
            throw e;
        }
    }

    /**
     * Execute request with Time Limiter
     * Ensures requests complete within specified timeout
     */
    @TimeLimiter(name = "gatewayTimeLimiter", fallbackMethod = "fallbackTimeLimiter")
    public <T> CompletableFuture<T> executeWithTimeLimiter(String serviceName, ServiceCall<T> serviceCall) {
        log.debug("Executing request to {} with time limiter", serviceName);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return serviceCall.call();
            } catch (Exception e) {
                log.error("Request to {} timed out: {}", serviceName, e.getMessage());
                throw new RuntimeException("Request timeout", e);
            }
        });
    }

    /**
     * Execute request with all resilience patterns
     * Combines circuit breaker, retry, and timeout
     */
    @CircuitBreaker(name = "gatewayCircuitBreaker", fallbackMethod = "fallbackComplete")
    @Retry(name = "gatewayRetry")
    @TimeLimiter(name = "gatewayTimeLimiter")
    public <T> CompletableFuture<T> executeWithAllPatterns(String serviceName, ServiceCall<T> serviceCall) {
        log.debug("Executing request to {} with all resilience patterns", serviceName);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return serviceCall.call();
            } catch (Exception e) {
                log.error("Request to {} failed: {}", serviceName, e.getMessage());
                throw new RuntimeException("Service call failed", e);
            }
        });
    }

    // Fallback Methods

    /**
     * Fallback for circuit breaker
     */
    private <T> T fallbackCircuitBreaker(String serviceName, ServiceCall<T> serviceCall, Exception ex) {
        log.error("Circuit breaker fallback triggered for {}: {}", serviceName, ex.getMessage());
        throw new RuntimeException("Service " + serviceName + " is currently unavailable. Circuit breaker is open.");
    }

    /**
     * Fallback for retry
     */
    private <T> T fallbackRetry(String serviceName, ServiceCall<T> serviceCall, Exception ex) {
        log.error("Retry fallback triggered for {}: {}", serviceName, ex.getMessage());
        throw new RuntimeException("Service " + serviceName + " failed after multiple retry attempts.");
    }

    /**
     * Fallback for time limiter
     */
    private <T> CompletableFuture<T> fallbackTimeLimiter(String serviceName, ServiceCall<T> serviceCall, Exception ex) {
        log.error("Time limiter fallback triggered for {}: {}", serviceName, ex.getMessage());
        return CompletableFuture.failedFuture(
                new RuntimeException("Service " + serviceName + " request timed out.")
        );
    }

    /**
     * Fallback for complete resilience pattern
     */
    private <T> CompletableFuture<T> fallbackComplete(String serviceName, ServiceCall<T> serviceCall, Exception ex) {
        log.error("Complete fallback triggered for {}: {}", serviceName, ex.getMessage());
        return CompletableFuture.failedFuture(
                new RuntimeException("Service " + serviceName + " is currently unavailable.")
        );
    }

    /**
     * Functional interface for service calls
     */
    @FunctionalInterface
    public interface ServiceCall<T> {
        T call() throws Exception;
    }
}
