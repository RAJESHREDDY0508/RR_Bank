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
        
        // Check if username already exists (case-sensitive)
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' already exists", request.getUsername());
            throw new UserAlreadyExistsException("Username already exists");
        }
        
        // Check if email already exists (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            log.warn("Registration failed: email '{}' already exists", request.getEmail());
            throw new UserAlreadyExistsException("Email already exists");
        }
        
        // Create new user - store email in lowercase for consistency
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail().toLowerCase().trim())
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
        
        // Create corresponding Customer record
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
     * Username is case-sensitive, email is case-insensitive.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: '{}'", request.getUsernameOrEmail());
        
        String input = request.getUsernameOrEmail().trim();
        
        // Find user by username (case-sensitive) or email (case-insensitive)
        User user = userRepository.findByUsername(input)
            .or(() -> userRepository.findByEmailIgnoreCase(input))
            .orElse(null);
        
        if (user == null) {
            log.warn("Login failed: User '{}' not found", input);
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        log.info("User found - Username: {}, Role: {}, Status: {}", 
                user.getUsername(), user.getRole(), user.getStatus());
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login failed: Account '{}' is locked until {}", 
                    user.getUsername(), user.getAccountLockedUntil());
            throw new AccountLockedException(
                "Account is temporarily locked. Please try again after " + user.getAccountLockedUntil()
            );
        }
        
        // Check if account is active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login failed: Account '{}' is not active (status: {})", 
                    user.getUsername(), user.getStatus());
            throw new AccountInactiveException(
                "Account is " + user.getStatus().name().toLowerCase() + ". Please contact support."
            );
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Password mismatch for user '{}'", user.getUsername());
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        try {
            // Authenticate with Spring Security (use username, not input)
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    request.getPassword()
                )
            );
            
            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
            }
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            log.info("User '{}' logged in successfully with role: {}", user.getUsername(), user.getRole());
            
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
                
        } catch (BadCredentialsException e) {
            log.error("BadCredentialsException for user '{}': {}", user.getUsername(), e.getMessage());
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }
    
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= 5) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), attempts);
        }
        
        userRepository.save(user);
    }
    
    /**
     * Refresh access token using a valid refresh token.
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Attempting to refresh token");
        
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
        
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid token type. Please provide a refresh token.");
        }
        
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AccountInactiveException("Account is no longer active");
        }
        
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
    
    public void logout(String userId) {
        log.info("User logged out: {}", userId);
    }
}
