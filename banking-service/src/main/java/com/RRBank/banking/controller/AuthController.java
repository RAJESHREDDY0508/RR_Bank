package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints
 * Handles user registration, login, and token refresh
 * 
 * All endpoints in this controller are PUBLIC (no authentication required).
 * CORS is handled globally in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Register a new user
     * POST /api/auth/register
     * 
     * @param request Registration details (username, email, password, etc.)
     * @return AuthResponse with access token and refresh token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        log.info("Registration successful for username: {}", request.getUsername());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Authenticate user and generate tokens
     * POST /api/auth/login
     * 
     * @param request Login credentials (username/email and password)
     * @return AuthResponse with access token and refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for user: {}", request.getUsernameOrEmail());
        AuthResponse response = authService.login(request);
        log.info("Login successful for user: {}", request.getUsernameOrEmail());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Refresh access token using refresh token
     * POST /api/auth/refresh
     * 
     * @param refreshToken The refresh token in Authorization header (Bearer token)
     * @return AuthResponse with new access token (and optionally new refresh token)
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        log.info("Token refresh request received");
        String token = refreshToken.replace("Bearer ", "");
        AuthResponse response = authService.refreshToken(token);
        log.info("Token refresh successful");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     * GET /api/auth/health
     * 
     * @return Simple status message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}
