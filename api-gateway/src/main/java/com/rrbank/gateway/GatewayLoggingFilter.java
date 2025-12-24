package com.RRBank.banking.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway Request/Response Logging Filter
 * Logs all incoming requests and outgoing responses for monitoring and debugging
 * Captures request details, response status, and execution time
 */
@Component
@Slf4j
@Order(3)
public class GatewayLoggingFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();
        
        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            // Log incoming request
            logRequest(wrappedRequest, requestId);
            
            // Process request
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log outgoing response
            logResponse(wrappedRequest, wrappedResponse, requestId, duration);
            
            // Important: copy content back to original response
            wrappedResponse.copyBodyToResponse();
        }
    }

    /**
     * Log incoming request details
     */
    private void logRequest(HttpServletRequest request, String requestId) {
        try {
            String username = getAuthenticatedUsername();
            String clientIP = getClientIP(request);
            
            Map<String, Object> requestLog = new HashMap<>();
            requestLog.put("requestId", requestId);
            requestLog.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
            requestLog.put("method", request.getMethod());
            requestLog.put("uri", request.getRequestURI());
            requestLog.put("queryString", request.getQueryString());
            requestLog.put("clientIP", clientIP);
            requestLog.put("username", username);
            requestLog.put("userAgent", request.getHeader("User-Agent"));
            
            // Log request headers (excluding sensitive ones)
            Map<String, String> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, request.getHeader(headerName));
                }
            }
            requestLog.put("headers", headers);
            
            log.info("Gateway Request - {}", requestLog);
            
        } catch (Exception e) {
            log.error("Error logging request", e);
        }
    }

    /**
     * Log outgoing response details
     */
    private void logResponse(
            HttpServletRequest request,
            ContentCachingResponseWrapper response,
            String requestId,
            long duration
    ) {
        try {
            Map<String, Object> responseLog = new HashMap<>();
            responseLog.put("requestId", requestId);
            responseLog.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
            responseLog.put("method", request.getMethod());
            responseLog.put("uri", request.getRequestURI());
            responseLog.put("status", response.getStatus());
            responseLog.put("duration", duration + "ms");
            responseLog.put("contentType", response.getContentType());
            responseLog.put("contentLength", response.getContentSize());
            
            // Log response headers
            Map<String, String> headers = new HashMap<>();
            Collection<String> headerNames = response.getHeaderNames();
            for (String headerName : headerNames) {
                if (!isSensitiveHeader(headerName)) {
                    headers.put(headerName, response.getHeader(headerName));
                }
            }
            responseLog.put("headers", headers);
            
            // Log response body for errors (4xx, 5xx)
            if (response.getStatus() >= 400) {
                String responseBody = getResponseBody(response);
                if (responseBody != null && !responseBody.isEmpty()) {
                    responseLog.put("responseBody", responseBody);
                }
            }
            
            // Determine log level based on status code
            if (response.getStatus() >= 500) {
                log.error("Gateway Response - {}", responseLog);
            } else if (response.getStatus() >= 400) {
                log.warn("Gateway Response - {}", responseLog);
            } else {
                log.info("Gateway Response - {}", responseLog);
            }
            
            // Log slow requests (> 1 second)
            if (duration > 1000) {
                log.warn("Gateway Slow Request - Duration: {}ms, URI: {}", duration, request.getRequestURI());
            }
            
        } catch (Exception e) {
            log.error("Error logging response", e);
        }
    }

    /**
     * Get authenticated username or "anonymous"
     */
    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal != null && !principal.equals("anonymousUser")) {
                return authentication.getName();
            }
        }
        return "anonymous";
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
     * Generate unique request ID
     */
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);
    }

    /**
     * Check if header contains sensitive information
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") ||
               lowerName.contains("password") ||
               lowerName.contains("token") ||
               lowerName.contains("secret") ||
               lowerName.contains("api-key") ||
               lowerName.contains("cookie");
    }

    /**
     * Get response body as string
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Error reading response body", e);
        }
        return null;
    }

    /**
     * Skip logging for static resources and actuator endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.endsWith(".css") ||
               uri.endsWith(".js") ||
               uri.endsWith(".ico") ||
               uri.endsWith(".png") ||
               uri.endsWith(".jpg") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/actuator/prometheus");
    }
}
