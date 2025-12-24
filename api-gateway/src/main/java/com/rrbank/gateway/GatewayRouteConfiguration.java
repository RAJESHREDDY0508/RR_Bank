package com.RRBank.banking.gateway;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * Gateway Route Configuration
 * Defines all API routes and their mappings
 * Maps incoming requests to appropriate service endpoints
 */
@Configuration
@ConfigurationProperties(prefix = "gateway")
@Data
@Slf4j
public class GatewayRouteConfiguration {

    private Map<String, RouteDefinition> routes = new HashMap<>();
    
    public GatewayRouteConfiguration() {
        initializeDefaultRoutes();
    }

    /**
     * Initialize default route mappings
     */
    private void initializeDefaultRoutes() {
        log.info("Initializing Gateway Route Configuration");
        
        // Auth Service Routes
        routes.put("auth", RouteDefinition.builder()
                .id("auth-service")
                .path("/api/auth/**")
                .serviceUrl("http://localhost:8080")
                .description("Authentication and Authorization Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(false) // No rate limit for auth
                .timeoutSeconds(5)
                .build());
        
        // Customer Service Routes
        routes.put("customers", RouteDefinition.builder()
                .id("customer-service")
                .path("/api/customers/**")
                .serviceUrl("http://localhost:8080")
                .description("Customer Management Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(true)
                .timeoutSeconds(10)
                .build());
        
        // Account Service Routes
        routes.put("accounts", RouteDefinition.builder()
                .id("account-service")
                .path("/api/accounts/**")
                .serviceUrl("http://localhost:8080")
                .description("Account Management Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(true)
                .timeoutSeconds(10)
                .build());
        
        // Transaction Service Routes
        routes.put("transactions", RouteDefinition.builder()
                .id("transaction-service")
                .path("/api/transactions/**")
                .serviceUrl("http://localhost:8080")
                .description("Transaction Processing Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(true)
                .timeoutSeconds(15)
                .build());
        
        // Payment Service Routes
        routes.put("payments", RouteDefinition.builder()
                .id("payment-service")
                .path("/api/payments/**")
                .serviceUrl("http://localhost:8080")
                .description("Payment Processing Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(true)
                .timeoutSeconds(20)
                .build());
        
        // Notification Service Routes
        routes.put("notifications", RouteDefinition.builder()
                .id("notification-service")
                .path("/api/notifications/**")
                .serviceUrl("http://localhost:8080")
                .description("Notification Management Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(false) // Don't retry notifications
                .rateLimitEnabled(true)
                .timeoutSeconds(5)
                .build());
        
        // Fraud Detection Service Routes
        routes.put("fraud", RouteDefinition.builder()
                .id("fraud-service")
                .path("/api/fraud/**")
                .serviceUrl("http://localhost:8080")
                .description("Fraud Detection Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(true)
                .timeoutSeconds(10)
                .build());
        
        // Statement Service Routes
        routes.put("statements", RouteDefinition.builder()
                .id("statement-service")
                .path("/api/statements/**")
                .serviceUrl("http://localhost:8080")
                .description("Statement Generation Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(false) // Don't retry statement generation
                .rateLimitEnabled(true)
                .timeoutSeconds(30)
                .build());
        
        // Audit Service Routes
        routes.put("audit", RouteDefinition.builder()
                .id("audit-service")
                .path("/api/audit/**")
                .serviceUrl("http://localhost:8080")
                .description("Audit Logging Service")
                .circuitBreakerEnabled(false) // Audit should always work
                .retryEnabled(false)
                .rateLimitEnabled(true)
                .timeoutSeconds(10)
                .build());
        
        log.info("Gateway Route Configuration completed - {} routes registered", routes.size());
        logRoutes();
    }

    /**
     * Get route by path
     */
    public Optional<RouteDefinition> getRouteByPath(String path) {
        return routes.values().stream()
                .filter(route -> matchesPath(path, route.getPath()))
                .findFirst();
    }

    /**
     * Check if path matches route pattern
     */
    private boolean matchesPath(String requestPath, String routePath) {
        String pattern = routePath.replace("/**", ".*");
        return requestPath.matches(pattern);
    }

    /**
     * Log all configured routes
     */
    private void logRoutes() {
        log.info("=== Gateway Route Mappings ===");
        routes.forEach((key, route) -> {
            log.info("  {} -> {} ({})",
                    route.getPath(),
                    route.getServiceUrl(),
                    route.getDescription()
            );
            log.info("    Circuit Breaker: {}, Retry: {}, Rate Limit: {}, Timeout: {}s",
                    route.isCircuitBreakerEnabled(),
                    route.isRetryEnabled(),
                    route.isRateLimitEnabled(),
                    route.getTimeoutSeconds()
            );
        });
        log.info("==============================");
    }

    /**
     * Route Definition
     */
    @Data
    @lombok.Builder
    public static class RouteDefinition {
        private String id;
        private String path;
        private String serviceUrl;
        private String description;
        private boolean circuitBreakerEnabled;
        private boolean retryEnabled;
        private boolean rateLimitEnabled;
        private int timeoutSeconds;
        private Map<String, String> metadata;
    }
}
