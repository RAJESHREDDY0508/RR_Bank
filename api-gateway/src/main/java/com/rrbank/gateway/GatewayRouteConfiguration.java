package com.rrbank.gateway;

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
                .serviceUrl("lb://BANKING-SERVICE")
                .description("Authentication and Authorization Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(false)
                .timeoutSeconds(5)
                .build());
        
        // Customer Service Routes
        routes.put("customers", RouteDefinition.builder()
                .id("customer-service")
                .path("/api/customers/**")
                .serviceUrl("lb://BANKING-SERVICE")
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
                .serviceUrl("lb://BANKING-SERVICE")
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
                .serviceUrl("lb://BANKING-SERVICE")
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
                .serviceUrl("lb://BANKING-SERVICE")
                .description("Payment Processing Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(true)
                .rateLimitEnabled(true)
                .timeoutSeconds(20)
                .build());
        
        // Statement Service Routes
        routes.put("statements", RouteDefinition.builder()
                .id("statement-service")
                .path("/api/statements/**")
                .serviceUrl("lb://BANKING-SERVICE")
                .description("Statement Generation Service")
                .circuitBreakerEnabled(true)
                .retryEnabled(false)
                .rateLimitEnabled(true)
                .timeoutSeconds(30)
                .build());
        
        log.info("Gateway Route Configuration completed - {} routes registered", routes.size());
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
