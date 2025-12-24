package com.RRBank.banking.gateway;

import com.RRBank.banking.security.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Gateway JWT Validation Filter
 * Validates JWT tokens for all incoming requests (except auth endpoints)
 * This acts as the first layer of security in the API Gateway
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class GatewayJwtValidationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        log.debug("Gateway Filter - Processing request: {} {}", method, requestURI);

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(requestURI, method)) {
            log.debug("Gateway Filter - Public endpoint, skipping JWT validation");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Gateway Filter - Missing or invalid Authorization header for: {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
                return;
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate token
                if (jwtUtil.validateToken(token)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Gateway Filter - JWT validated successfully for user: {}", username);
                } else {
                    log.warn("Gateway Filter - Invalid JWT token for: {}", requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid or expired JWT token\"}");
                    return;
                }
            }

            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Gateway Filter - Error processing JWT: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication error: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Check if the endpoint is public (doesn't require authentication)
     */
    private boolean isPublicEndpoint(String uri, String method) {
        // Public endpoints that don't require JWT validation
        return uri.equals("/") ||
               uri.startsWith("/api/auth/login") ||
               uri.startsWith("/api/auth/register") ||
               uri.startsWith("/api/auth/refresh") ||
               uri.startsWith("/h2-console") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/actuator/info") ||
               uri.startsWith("/actuator/prometheus") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.endsWith(".html") ||
               uri.endsWith(".css") ||
               uri.endsWith(".js") ||
               uri.endsWith(".ico");
    }
}
