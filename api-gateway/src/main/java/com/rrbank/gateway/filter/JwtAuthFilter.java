package com.rrbank.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> OPEN_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/verify-email",
            "/api/auth/resend-verification",
            "/api/admin/auth/login",
            "/api/admin/auth/refresh",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        HttpMethod method = exchange.getRequest().getMethod();
        
        log.debug("Processing request: {} {}", method, path);
        
        // Allow CORS preflight requests
        if (method == HttpMethod.OPTIONS) {
            log.debug("Allowing OPTIONS preflight request for path: {}", path);
            return chain.filter(exchange);
        }
        
        // Check if path is open (public)
        if (isOpenPath(path)) {
            log.debug("Open path accessed, bypassing JWT validation: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        
        try {
            Claims claims = validateToken(token);
            
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            
            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
            
            if (userId != null) {
                requestBuilder.header("X-User-Id", userId);
            }
            if (email != null) {
                requestBuilder.header("X-User-Email", email);
            }
            if (role != null) {
                requestBuilder.header("X-User-Role", role);
            }
            
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
            
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for path {}: {}", path, e.getMessage());
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return handleUnauthorized(exchange, "Invalid token");
        }
    }

    private boolean isOpenPath(String path) {
        // Normalize path by removing trailing slash
        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        
        boolean isOpen = OPEN_PATHS.stream().anyMatch(openPath -> 
            normalizedPath.equals(openPath) || normalizedPath.startsWith(openPath + "/") || normalizedPath.startsWith(openPath)
        );
        
        if (isOpen) {
            log.debug("Path '{}' matched as open path", path);
        }
        
        return isOpen;
    }
    
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", message);
        return exchange.getResponse().setComplete();
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
