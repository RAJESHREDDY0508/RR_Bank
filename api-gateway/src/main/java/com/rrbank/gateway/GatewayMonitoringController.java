package com.RRBank.banking.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gateway Monitoring Controller
 * Provides endpoints to monitor gateway health, routes, and resilience status
 */
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
@Slf4j
public class GatewayMonitoringController {

    private final GatewayRouteConfiguration routeConfiguration;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get gateway health status
     * GET /api/gateway/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getGatewayHealth() {
        log.debug("Getting gateway health status");
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("gateway", "RR-Bank API Gateway");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get all configured routes
     * GET /api/gateway/routes
     */
    @GetMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRoutes() {
        log.info("REST request to get all gateway routes");
        
        List<Map<String, Object>> routeList = routeConfiguration.getRoutes().values().stream()
                .map(route -> {
                    Map<String, Object> routeInfo = new HashMap<>();
                    routeInfo.put("id", route.getId());
                    routeInfo.put("path", route.getPath());
                    routeInfo.put("serviceUrl", route.getServiceUrl());
                    routeInfo.put("description", route.getDescription());
                    routeInfo.put("circuitBreakerEnabled", route.isCircuitBreakerEnabled());
                    routeInfo.put("retryEnabled", route.isRetryEnabled());
                    routeInfo.put("rateLimitEnabled", route.isRateLimitEnabled());
                    routeInfo.put("timeoutSeconds", route.getTimeoutSeconds());
                    return routeInfo;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalRoutes", routeList.size());
        response.put("routes", routeList);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get specific route information
     * GET /api/gateway/routes/{routeId}
     */
    @GetMapping("/routes/{routeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRoute(@PathVariable String routeId) {
        log.info("REST request to get route: {}", routeId);
        
        Optional<GatewayRouteConfiguration.RouteDefinition> route = 
                routeConfiguration.getRoutes().values().stream()
                        .filter(r -> r.getId().equals(routeId))
                        .findFirst();
        
        if (route.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        GatewayRouteConfiguration.RouteDefinition routeDef = route.get();
        Map<String, Object> routeInfo = new HashMap<>();
        routeInfo.put("id", routeDef.getId());
        routeInfo.put("path", routeDef.getPath());
        routeInfo.put("serviceUrl", routeDef.getServiceUrl());
        routeInfo.put("description", routeDef.getDescription());
        routeInfo.put("circuitBreakerEnabled", routeDef.isCircuitBreakerEnabled());
        routeInfo.put("retryEnabled", routeDef.isRetryEnabled());
        routeInfo.put("rateLimitEnabled", routeDef.isRateLimitEnabled());
        routeInfo.put("timeoutSeconds", routeDef.getTimeoutSeconds());
        
        return ResponseEntity.ok(routeInfo);
    }

    /**
     * Get circuit breaker status for all services
     * GET /api/gateway/circuit-breakers
     */
    @GetMapping("/circuit-breakers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCircuitBreakers() {
        log.info("REST request to get circuit breaker status");
        
        List<Map<String, Object>> circuitBreakers = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .map(cb -> {
                    Map<String, Object> cbInfo = new HashMap<>();
                    cbInfo.put("name", cb.getName());
                    cbInfo.put("state", cb.getState().toString());
                    
                    CircuitBreaker.Metrics metrics = cb.getMetrics();
                    Map<String, Object> metricsInfo = new HashMap<>();
                    metricsInfo.put("failureRate", metrics.getFailureRate());
                    metricsInfo.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
                    metricsInfo.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
                    metricsInfo.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
                    metricsInfo.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
                    
                    cbInfo.put("metrics", metricsInfo);
                    return cbInfo;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalCircuitBreakers", circuitBreakers.size());
        response.put("circuitBreakers", circuitBreakers);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get circuit breaker status for specific service
     * GET /api/gateway/circuit-breakers/{name}
     */
    @GetMapping("/circuit-breakers/{name}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCircuitBreaker(@PathVariable String name) {
        log.info("REST request to get circuit breaker: {}", name);
        
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            
            Map<String, Object> cbInfo = new HashMap<>();
            cbInfo.put("name", circuitBreaker.getName());
            cbInfo.put("state", circuitBreaker.getState().toString());
            
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            Map<String, Object> metricsInfo = new HashMap<>();
            metricsInfo.put("failureRate", metrics.getFailureRate());
            metricsInfo.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
            metricsInfo.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            metricsInfo.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());
            metricsInfo.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
            
            cbInfo.put("metrics", metricsInfo);
            cbInfo.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(cbInfo);
        } catch (Exception e) {
            log.error("Error getting circuit breaker {}: {}", name, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reset circuit breaker
     * POST /api/gateway/circuit-breakers/{name}/reset
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker(@PathVariable String name) {
        log.info("REST request to reset circuit breaker: {}", name);
        
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            circuitBreaker.reset();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Circuit breaker reset successfully");
            response.put("name", name);
            response.put("state", circuitBreaker.getState().toString());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting circuit breaker {}: {}", name, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Circuit breaker not found or reset failed");
            error.put("name", name);
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get gateway statistics
     * GET /api/gateway/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getGatewayStats() {
        log.info("REST request to get gateway statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRoutes", routeConfiguration.getRoutes().size());
        stats.put("totalCircuitBreakers", circuitBreakerRegistry.getAllCircuitBreakers().stream().count());
        
        // Count circuit breakers by state
        Map<String, Long> cbStateCount = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .collect(Collectors.groupingBy(
                        cb -> cb.getState().toString(),
                        Collectors.counting()
                ));
        stats.put("circuitBreakerStates", cbStateCount);
        
        // Calculate total metrics
        int totalSuccessfulCalls = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .mapToInt(cb -> (int) cb.getMetrics().getNumberOfSuccessfulCalls())
                .sum();
        
        int totalFailedCalls = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .mapToInt(cb -> (int) cb.getMetrics().getNumberOfFailedCalls())
                .sum();
        
        stats.put("totalSuccessfulCalls", totalSuccessfulCalls);
        stats.put("totalFailedCalls", totalFailedCalls);
        stats.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }
}
