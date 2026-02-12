package com.rrbank.auth.service;

import com.rrbank.auth.dto.AuthDTOs.*;
import com.rrbank.auth.entity.RefreshToken;
import com.rrbank.auth.entity.User;
import com.rrbank.auth.event.UserEventProducer;
import com.rrbank.auth.repository.RefreshTokenRepository;
import com.rrbank.auth.repository.UserRepository;
import com.rrbank.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserEventProducer userEventProducer;
    private final NotificationServiceClient notificationClient;
    private final CustomerServiceClient customerServiceClient;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.UserRole.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        // Create customer record in customer-service
        try {
            customerServiceClient.createCustomer(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                request.getPhoneNumber()
            );
            log.info("Customer record created for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to create customer record (non-fatal): {}", e.getMessage());
        }

        // Publish user created event (for other services via Kafka if enabled)
        try {
            userEventProducer.publishUserCreated(user);
        } catch (Exception e) {
            log.warn("Failed to publish user created event (non-fatal): {}", e.getMessage());
        }

        // Send welcome email
        try {
            notificationClient.sendWelcomeEmail(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName()
            );
        } catch (Exception e) {
            log.warn("Failed to send welcome email (non-fatal): {}", e.getMessage());
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(), 
                request.getUsernameOrEmail()
        ).orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.isAccountLocked()) {
            throw new RuntimeException("Account is locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            
            // Send security alert for failed login attempt after multiple failures
            if (user.getFailedLoginAttempts() >= 3) {
                try {
                    notificationClient.sendSecurityAlert(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getFirstName(),
                        "Multiple Failed Login Attempts",
                        userAgent,
                        ipAddress,
                        null
                    );
                } catch (Exception e) {
                    log.warn("Failed to send security alert: {}", e.getMessage());
                }
            }
            
            throw new RuntimeException("Invalid credentials");
        }

        user.resetFailedAttempts();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Ensure customer record exists (in case it was missed during registration)
        try {
            if (!customerServiceClient.customerExists(user.getId())) {
                customerServiceClient.createCustomer(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    null
                );
                log.info("Created missing customer record for user: {}", user.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to verify/create customer record (non-fatal): {}", e.getMessage());
        }

        // Send security alert for new login
        try {
            notificationClient.sendSecurityAlert(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                "New Login",
                userAgent,
                ipAddress,
                null
            );
        } catch (Exception e) {
            log.warn("Failed to send login notification: {}", e.getMessage());
        }

        log.info("User logged in successfully: {}", user.getId());
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        log.info("Refreshing token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(UUID userId) {
        log.info("Logging out user: {}", userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public TokenValidationResponse validateToken(String token) {
        try {
            Claims claims = jwtTokenProvider.validateToken(token);
            return TokenValidationResponse.builder()
                    .valid(true)
                    .userId(claims.getSubject())
                    .email(claims.get("email", String.class))
                    .role(claims.get("role", String.class))
                    .build();
        } catch (Exception e) {
            return TokenValidationResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Initiating password reset for: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        
        // TODO: Store reset token in database with expiry
        
        // Send password reset email
        try {
            notificationClient.sendPasswordResetEmail(
                user.getEmail(),
                user.getFirstName(),
                resetToken
            );
        } catch (Exception e) {
            log.warn("Failed to send password reset email: {}", e.getMessage());
        }
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .user(UserInfo.builder()
                        .id(user.getId().toString())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
