package com.RRBank.banking.service;

import com.RRBank.banking.dto.AuthResponse;
import com.RRBank.banking.dto.LoginRequest;
import com.RRBank.banking.dto.RegisterRequest;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.repository.UserRepository;
import com.RRBank.banking.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService
 * Tests authentication, registration, and token management
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .role(User.UserRole.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .build();

        // LoginRequest uses usernameOrEmail, not username
        loginRequest = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

        registerRequest = RegisterRequest.builder()
                .username("newuser")
                .email("new@example.com")
                .password("password123")
                .firstName("New")
                .lastName("User")
                .build();
    }

    @Test
    void testLogin_Success() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(any(User.class)))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class)))
                .thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationTime())
                .thenReturn(3600000L);
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(testUser.getUserId(), response.getUserId());
        assertEquals(testUser.getUsername(), response.getUsername());

        verify(userRepository).findByUsername("testuser");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(testUser);
        verify(jwtTokenProvider).generateRefreshToken(testUser);
    }

    @Test
    void testLogin_InvalidCredentials_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.save(any(User.class)))
                .thenReturn(testUser);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLogin_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);

        // Act
        AuthResponse result = authService.register(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));

        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRefreshToken_Success() {
        // Arrange
        String oldRefreshToken = "old-refresh-token";
        String username = "testuser";

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(testUser)).thenReturn("new-access-token");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600000L);

        // Act
        AuthResponse response = authService.refreshToken(oldRefreshToken);

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals(oldRefreshToken, response.getRefreshToken()); // refresh token is reused

        verify(jwtTokenProvider).validateToken(oldRefreshToken);
        verify(jwtTokenProvider).getUsernameFromToken(oldRefreshToken);
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testRefreshToken_InvalidToken_ThrowsException() {
        // Arrange
        String invalidToken = "invalid-token";
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.refreshToken(invalidToken));

        verify(jwtTokenProvider).validateToken(invalidToken);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void testRefreshToken_UserNotFound_ThrowsException() {
        // Arrange
        String validToken = "valid-token";
        String username = "testuser";

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> authService.refreshToken(validToken));

        verify(userRepository).findByUsername(username);
    }
}
