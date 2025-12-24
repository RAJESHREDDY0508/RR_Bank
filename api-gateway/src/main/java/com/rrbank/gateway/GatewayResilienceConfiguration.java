package com.RRBank.banking.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Gateway Circuit Breaker Configuration
 * Configures Resilience4j for fault tolerance
 * Implements Circuit Breaker, Retry, and Time Limiter patterns
 */
@Configuration
@Slf4j
public class GatewayResilienceConfiguration {

    /**
     * Circuit Breaker Registry with custom configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Failure rate threshold (percentage)
                .failureRateThreshold(50)
                // Minimum number of calls before calculating failure rate
                .minimumNumberOfCalls(10)
                // Number of permitted calls in half-open state
                .permittedNumberOfCallsInHalfOpenState(5)
                // Wait duration before transitioning to half-open
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // Sliding window size for recording outcomes
                .slidingWindowSize(100)
                // Record exceptions as failures
                .recordExceptions(Exception.class)
                // Ignore specific exceptions
                // .ignoreExceptions(BusinessException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        
        // Add event listeners for monitoring
        registry.getEventPublisher().onEntryAdded(event -> {
            String name = event.getAddedEntry().getName();
            log.info("Circuit Breaker added: {}", name);
            
            event.getAddedEntry().getEventPublisher()
                    .onStateTransition(e -> log.warn("Circuit Breaker {} state changed from {} to {}",
                            name, e.getStateTransition().getFromState(), e.getStateTransition().getToState()))
                    .onError(e -> log.error("Circuit Breaker {} recorded error: {}",
                            name, e.getThrowable().getMessage()))
                    .onSuccess(e -> log.debug("Circuit Breaker {} recorded success",
                            name));
        });
        
        return registry;
    }

    /**
     * Retry Registry with custom configuration
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                // Maximum number of retry attempts
                .maxAttempts(3)
                // Wait duration between retries
                .waitDuration(Duration.ofMillis(500))
                // Exponential backoff
                .intervalFunction(attempt -> {
                    long waitTime = (long) Math.pow(2, attempt) * 500; // Exponential backoff
                    return Math.min(waitTime, 5000); // Max 5 seconds
                })
                // Retry on specific exceptions
                .retryExceptions(Exception.class)
                // Don't retry on specific exceptions
                // .ignoreExceptions(BusinessException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        
        // Add event listeners
        registry.getEventPublisher().onEntryAdded(event -> {
            String name = event.getAddedEntry().getName();
            log.info("Retry configuration added: {}", name);
            
            event.getAddedEntry().getEventPublisher()
                    .onRetry(e -> log.warn("Retry {} attempt #{} after error: {}",
                            name, e.getNumberOfRetryAttempts(), e.getLastThrowable().getMessage()))
                    .onSuccess(e -> log.debug("Retry {} succeeded after {} attempts",
                            name, e.getNumberOfRetryAttempts()));
        });
        
        return registry;
    }

    /**
     * Time Limiter Registry with custom configuration
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                // Timeout duration
                .timeoutDuration(Duration.ofSeconds(10))
                // Cancel running future on timeout
                .cancelRunningFuture(true)
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
        
        // Add event listeners
        registry.getEventPublisher().onEntryAdded(event -> {
            String name = event.getAddedEntry().getName();
            log.info("Time Limiter configuration added: {}", name);
            
            event.getAddedEntry().getEventPublisher()
                    .onTimeout(e -> log.warn("Time Limiter {} timed out", name))
                    .onSuccess(e -> log.debug("Time Limiter {} completed successfully", name));
        });
        
        return registry;
    }
}
