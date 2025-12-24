package com.RRBank.banking.service;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.exception.*;
import com.RRBank.banking.repository.UserRepository;
import com.RRBank.banking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication Service
 * 
 * <p>Handles user authentication, registration, and JWT token management.</p>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>User registration with duplicate detection</li>
 *   <li>Login with username or email</li>
 *   <li>Account lockout after failed attempts</li>
 *   <li>JWT access and refresh token generation</li>
 *   <li>Token refresh functionality</li>
 * </ul>
 * 
 * <h2>Security Features:</h2>
 * <ul>
 *   <li>Password hashing with BCrypt</li>
 *   <li>Account lockout after 5 failed attempts (30 min)</li>
 *   <li>Generic error messages to prevent enumeration</li>
 * </ul>
 * 
 * @author RR-Bank Development Team
 * @version 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    /**
     * Register a new user account.
     * 
     * @param request Registration details
     * @return AuthResponse with JWT tokens
     * @throws UserAlreadyExistsException if username or email already exists
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
            .build();
    }
    
    /**
     * Authenticate user and generate JWT tokens.
     * 
     * @param request Login credentials (username/email and password)
     * @return AuthResponse with JWT tokens
     * @throws InvalidCredentialsException if credentials are invalid
     * @throws AccountLockedException if account is locked
     * @throws AccountInactiveException if account is not active
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsernameOrEmail());
        
        // Find user by username or email
        // Note: Using generic error message to prevent username enumeration
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
            .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
            .orElseThrow(() -> {
                log.warn("Login failed: user '{}' not found", request.getUsernameOrEmail());
                return new InvalidCredentialsException();
            });
        
        // Check if account is locked
        if (user.isAccountLocked()) {
            log.warn("Login failed: account '{}' is locked until {}", 
                    user.getUsername(), user.getAccountLockedUntil());
            throw new AccountLockedException(
                "Account is temporarily locked due to multiple failed login attempts. " +
                "Please try again after " + user.getAccountLockedUntil()
            );
        }
        
        // Check if account is active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            log.warn("Login failed: account '{}' is not active (status: {})", 
                    user.getUsername(), user.getStatus());
            throw new AccountInactiveException(
                "Account is " + user.getStatus().name().toLowerCase() + ". Please contact support."
            );
        }
        
        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );
            
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
            
            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
                
        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            
            // Lock account after 5 failed attempts
            if (attempts >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), attempts);
            }
            
            userRepository.save(user);
            
            log.warn("Login failed for '{}': invalid password (attempt {})", 
                    user.getUsername(), attempts);
            
            // Throw generic error to prevent enumeration
            throw new InvalidCredentialsException();
        }
    }
    
    /**
     * Refresh access token using a valid refresh token.
     * 
     * @param refreshToken The refresh token
     * @return AuthResponse with new access token
     * @throws InvalidTokenException if refresh token is invalid
     * @throws ResourceNotFoundException if user not found
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
            .refreshToken(refreshToken) // Return same refresh token
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getExpirationTime())
            .userId(user.getUserId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();
    }
    
    /**
     * Logout user (invalidate tokens).
     * 
     * <p>Note: With stateless JWT, we can't truly invalidate tokens on the server.
     * Client should discard tokens. For true logout, implement token blacklisting.</p>
     * 
     * @param userId The user ID to logout
     */
    public void logout(String userId) {
        log.info("User logged out: {}", userId);
        // In a production system, you might:
        // 1. Add token to blacklist (Redis)
        // 2. Increment token version in user record
        // 3. Clear server-side sessions if using hybrid approach
    }
}
