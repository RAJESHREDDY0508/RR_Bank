package com.RRBank.banking.gateway;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway Rate Limiting Filter
 * Implements rate limiting per user and per IP address
 * Uses Token Bucket algorithm via Bucket4j
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class GatewayRateLimitingFilter extends OncePerRequestFilter {

    // Rate limit configurations
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;
    private static final int AUTHENTICATED_REQUESTS_PER_MINUTE = 200;
    private static final int ADMIN_REQUESTS_PER_MINUTE = 500;
    
    // Cache for storing buckets per user/IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String clientIdentifier = getClientIdentifier(request);
        
        log.debug("Rate Limit Filter - Checking limits for: {}", clientIdentifier);

        // Skip rate limiting for health checks and actuator endpoints
        if (isExemptEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = resolveBucket(clientIdentifier, request);
        
        if (bucket.tryConsume(1)) {
            // Request allowed
            long remainingTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            response.setHeader("X-Rate-Limit-Identifier", clientIdentifier);
            
            log.debug("Rate Limit Filter - Request allowed. Remaining tokens: {}", remainingTokens);
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            log.warn("Rate Limit Filter - Rate limit exceeded for: {}", clientIdentifier);
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", "60");
            response.getWriter().write(String.format(
                    "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\",\"identifier\":\"%s\"}",
                    clientIdentifier
            ));
        }
    }

    /**
     * Get client identifier (username for authenticated users, IP for anonymous)
     */
    private String getClientIdentifier(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            return "user:" + authentication.getName();
        }
        
        // For anonymous users, use IP address
        String ipAddress = getClientIP(request);
        return "ip:" + ipAddress;
    }

    /**
     * Get client IP address considering proxies
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Resolve or create a rate limit bucket for the client
     */
    private Bucket resolveBucket(String key, HttpServletRequest request) {
        return cache.computeIfAbsent(key, k -> createNewBucket(request));
    }

    /**
     * Create a new bucket with appropriate rate limits based on user role
     */
    private Bucket createNewBucket(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        int requestsPerMinute = DEFAULT_REQUESTS_PER_MINUTE;
        
        if (authentication != null && authentication.isAuthenticated()) {
            boolean hasAdminRole = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (hasAdminRole) {
                requestsPerMinute = ADMIN_REQUESTS_PER_MINUTE;
                log.debug("Rate Limit Filter - Creating bucket for ADMIN with {} requests/minute", requestsPerMinute);
            } else {
                requestsPerMinute = AUTHENTICATED_REQUESTS_PER_MINUTE;
                log.debug("Rate Limit Filter - Creating bucket for authenticated user with {} requests/minute", requestsPerMinute);
            }
        } else {
            log.debug("Rate Limit Filter - Creating bucket for anonymous user with {} requests/minute", requestsPerMinute);
        }
        
        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Check if endpoint is exempt from rate limiting
     */
    private boolean isExemptEndpoint(String uri) {
        return uri.startsWith("/actuator/health") ||
               uri.startsWith("/actuator/info") ||
               uri.startsWith("/actuator/prometheus");
    }
}
