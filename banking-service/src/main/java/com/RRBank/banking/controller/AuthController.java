package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.repository.UserRepository;
import com.RRBank.banking.security.CustomUserDetails;
import com.RRBank.banking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for authentication endpoints
 * Handles user registration, login, and token refresh
 * 
 * All endpoints in this controller are PUBLIC (no authentication required),
 * except /me which requires authentication.
 * CORS is handled globally in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthService authService;
    private final UserRepository userRepository;
    
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
        log.info("Login successful for user: {} with role: {}", request.getUsernameOrEmail(), response.getRole());
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
     * Get current user information
     * GET /api/auth/me
     * 
     * This endpoint requires authentication.
     * 
     * @return Current user information including role
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        Map<String, Object> userInfo = new HashMap<>();
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            userInfo.put("userId", userDetails.getUserId());
            userInfo.put("username", userDetails.getUsername());
            userInfo.put("email", userDetails.getEmail());
            userInfo.put("roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            userInfo.put("isAdmin", authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
            
            // Get additional user info from database
            userRepository.findById(userDetails.getUserId()).ifPresent(user -> {
                userInfo.put("firstName", user.getFirstName());
                userInfo.put("lastName", user.getLastName());
                userInfo.put("status", user.getStatus().name());
            });
        } else {
            userInfo.put("username", authentication.getName());
            userInfo.put("roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        }
        
        return ResponseEntity.ok(userInfo);
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
