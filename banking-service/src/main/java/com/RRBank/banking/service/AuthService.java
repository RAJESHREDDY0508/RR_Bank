package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.Customer;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.exception.*;
import com.RRBank.banking.repository.CustomerRepository;
import com.RRBank.banking.repository.UserRepository;
import com.RRBank.banking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication Service
 * Handles user authentication, registration, and JWT token management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    /**
     * Register a new user account.
     * Also creates a corresponding Customer record for banking operations.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' already exists", request.getUsername());
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email '{}' already exists", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }
        
        // Create new user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .address(request.getAddress())
            .city(request.getCity())
            .state(request.getState())
            .postalCode(request.getPostalCode())
            .country(request.getCountry())
            .role(User.UserRole.CUSTOMER)
            .status(User.UserStatus.ACTIVE)
            .kycVerified(false)
            .failedLoginAttempts(0)
            .build();
        
        user = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", user.getUsername(), user.getUserId());
        
        // ✅ CRITICAL: Create corresponding Customer record
        try {
            Customer customer = Customer.builder()
                .userId(UUID.fromString(user.getUserId()))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhoneNumber())
                .address(user.getAddress())
                .city(user.getCity())
                .state(user.getState())
                .zipCode(user.getPostalCode())
                .country(user.getCountry())
                .kycStatus(Customer.KycStatus.PENDING)
                .customerSegment(Customer.CustomerSegment.REGULAR)
                .build();
            
            customer = customerRepository.save(customer);
            log.info("Customer record created: {} for user {}", customer.getId(), user.getUserId());
        } catch (Exception e) {
            log.error("Failed to create Customer record for user {}: {}", user.getUserId(), e.getMessage());
            // Don't fail registration - customer can be created later
        }
        
        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getExpirationTime())
            .userId(user.getUserId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .success(true)
            .build();
    }
    
    /**
     * Authenticate user and generate JWT tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("=== LOGIN ATTEMPT ===");
        log.info("Attempting login for: '{}'", request.getUsernameOrEmail());
        
        // Find user by username or email
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
            .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
            .orElse(null);
        
        if (user == null) {
            log.warn("LOGIN FAILED: User '{}' not found in database", request.getUsernameOrEmail());
            
            // List all users for debugging
            long userCount = userRepository.count();
            log.info("Total users in database: {}", userCount);
            
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        log.info("User found - ID: {}, Username: {}, Email: {}, Role: {}, Status: {}", 
                user.getUserId(), user.getUsername(), user.getEmail(), user.getRole(), user.getStatus());
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("LOGIN FAILED: Account '{}' is locked until {}", 
                    user.getUsername(), user.getAccountLockedUntil());
            throw new AccountLockedException(
                "Account is temporarily locked due to multiple failed login attempts. " +
                "Please try again after " + user.getAccountLockedUntil()
            );
        }
        
        // Check if account is active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("LOGIN FAILED: Account '{}' is not active (status: {})", 
                    user.getUsername(), user.getStatus());
            throw new AccountInactiveException(
                "Account is " + user.getStatus().name().toLowerCase() + ". Please contact support."
            );
        }
        
        // Verify password manually first for debugging
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        log.info("Manual password verification: {}", passwordMatches ? "MATCH" : "NO MATCH");
        
        if (!passwordMatches) {
            log.warn("LOGIN FAILED: Password does not match for user '{}'", user.getUsername());
            
            // Increment failed login attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            
            if (attempts >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), attempts);
            }
            
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        try {
            // Authenticate with Spring Security
            log.info("Attempting Spring Security authentication...");
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),  // Use username, not the input
                    request.getPassword()
                )
            );
            log.info("Spring Security authentication successful");
            
            // Authentication successful - reset failed attempts
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
            }
            
            // Update last login timestamp
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            log.info("User '{}' logged in successfully", user.getUsername());
            
            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);
            
            log.info("JWT tokens generated successfully");
            
            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .success(true)
                .build();
                
        } catch (BadCredentialsException e) {
            log.error("Spring Security BadCredentialsException: {}", e.getMessage());
            
            // Increment failed login attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            
            if (attempts >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), attempts);
            }
            
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid username or password");
            
        } catch (Exception e) {
            log.error("Unexpected authentication error: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new InvalidCredentialsException("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Refresh access token using a valid refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Attempting to refresh token");
        
        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Token refresh failed: invalid refresh token");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        
        // Verify it's a refresh token (not access token)
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("Token refresh failed: provided token is not a refresh token");
            throw new InvalidTokenException("Invalid token type. Please provide a refresh token.");
        }
        
        // Get username from token
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        // Find user
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.warn("Token refresh failed: user '{}' not found", username);
                return new ResourceNotFoundException("User not found");
            });
        
        // Check if account is still active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Token refresh failed: account '{}' is not active", username);
            throw new AccountInactiveException("Account is no longer active");
        }
        
        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateToken(user);
        
        log.info("Token refreshed successfully for user: {}", username);
        
        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getExpirationTime())
            .userId(user.getUserId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .success(true)
            .build();
    }
    
    /**
     * Logout user (invalidate tokens).
     */
    public void logout(String userId) {
        log.info("User logged out: {}", userId);
    }
}
