package com.rrbank.admin.service;

import com.rrbank.admin.dto.AdminAuthDTOs.*;
import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.entity.AdminRefreshToken;
import com.rrbank.admin.entity.AdminUser;
import com.rrbank.admin.entity.Role;
import com.rrbank.admin.exception.AdminNotFoundException;
import com.rrbank.admin.exception.BusinessException;
import com.rrbank.admin.repository.AdminRefreshTokenRepository;
import com.rrbank.admin.repository.AdminUserRepository;
import com.rrbank.admin.repository.RoleRepository;
import com.rrbank.admin.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final AdminRefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        AdminUser admin = adminUserRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (admin.isAccountLocked()) {
            auditLogService.logAction(
                    admin.getId(),
                    admin.getUsername(),
                    "LOGIN_ATTEMPT_LOCKED",
                    AdminAuditLog.ActionType.LOGIN,
                    "ADMIN_USER",
                    admin.getId().toString(),
                    "Login attempt on locked account",
                    ipAddress,
                    userAgent,
                    AdminAuditLog.AuditStatus.FAILURE,
                    "Account is locked"
            );
            throw new LockedException("Account is locked. Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            admin.incrementFailedAttempts();
            adminUserRepository.save(admin);
            
            auditLogService.logAction(
                    admin.getId(),
                    admin.getUsername(),
                    "LOGIN_FAILED",
                    AdminAuditLog.ActionType.LOGIN,
                    "ADMIN_USER",
                    admin.getId().toString(),
                    "Failed login attempt",
                    ipAddress,
                    userAgent,
                    AdminAuditLog.AuditStatus.FAILURE,
                    "Invalid password"
            );
            
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset failed attempts on successful login
        admin.resetFailedAttempts();
        admin.setLastLogin(LocalDateTime.now());
        admin.setLastLoginIp(ipAddress);
        adminUserRepository.save(admin);

        // Get roles and permissions
        Set<String> roleNames = admin.getRoleNames();
        Set<String> permissionNames = admin.getPermissionNames();

        // Generate tokens with roles and permissions
        String accessToken = jwtTokenProvider.generateAccessToken(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                roleNames,
                permissionNames
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getId());

        // Save refresh token
        AdminRefreshToken tokenEntity = AdminRefreshToken.builder()
                .token(refreshToken)
                .adminUserId(admin.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(tokenEntity);

        // Log successful login
        auditLogService.logAction(
                admin.getId(),
                admin.getUsername(),
                "LOGIN_SUCCESS",
                AdminAuditLog.ActionType.LOGIN,
                "ADMIN_USER",
                admin.getId().toString(),
                "Successful login",
                ipAddress,
                userAgent,
                AdminAuditLog.AuditStatus.SUCCESS,
                null
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .user(AdminUserInfo.builder()
                        .userId(admin.getId().toString())
                        .username(admin.getUsername())
                        .email(admin.getEmail())
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .role(admin.getPrimaryRoleName())
                        .roles(roleNames)
                        .permissions(permissionNames)
                        .department(admin.getDepartment())
                        .mustChangePassword(admin.getMustChangePassword())
                        .build())
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        AdminRefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid refresh token"));

        if (!tokenEntity.isValid()) {
            throw new BusinessException("TOKEN_EXPIRED", "Refresh token has expired");
        }

        AdminUser admin = adminUserRepository.findById(tokenEntity.getAdminUserId())
                .orElseThrow(() -> new AdminNotFoundException("Admin user not found"));

        // Revoke old token
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // Get roles and permissions
        Set<String> roleNames = admin.getRoleNames();
        Set<String> permissionNames = admin.getPermissionNames();

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                admin.getId(),
                admin.getUsername(),
                admin.getEmail(),
                roleNames,
                permissionNames
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(admin.getId());

        // Save new refresh token
        AdminRefreshToken newTokenEntity = AdminRefreshToken.builder()
                .token(newRefreshToken)
                .adminUserId(admin.getId())
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpiration() / 1000))
                .ipAddress(tokenEntity.getIpAddress())
                .userAgent(tokenEntity.getUserAgent())
                .build();
        refreshTokenRepository.save(newTokenEntity);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration() / 1000)
                .user(AdminUserInfo.builder()
                        .userId(admin.getId().toString())
                        .username(admin.getUsername())
                        .email(admin.getEmail())
                        .firstName(admin.getFirstName())
                        .lastName(admin.getLastName())
                        .role(admin.getPrimaryRoleName())
                        .roles(roleNames)
                        .permissions(permissionNames)
                        .department(admin.getDepartment())
                        .mustChangePassword(admin.getMustChangePassword())
                        .build())
                .build();
    }

    @Transactional
    public void logout(UUID adminUserId, String ipAddress, String userAgent) {
        refreshTokenRepository.revokeAllByAdminUserId(adminUserId);

        AdminUser admin = adminUserRepository.findById(adminUserId).orElse(null);
        if (admin != null) {
            auditLogService.logAction(
                    adminUserId,
                    admin.getUsername(),
                    "LOGOUT",
                    AdminAuditLog.ActionType.LOGOUT,
                    "ADMIN_USER",
                    adminUserId.toString(),
                    "Admin logout",
                    ipAddress,
                    userAgent,
                    AdminAuditLog.AuditStatus.SUCCESS,
                    null
            );
        }
    }

    public AdminUserResponse getCurrentAdmin(UUID adminUserId) {
        AdminUser admin = adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new AdminNotFoundException("Admin user not found"));
        return AdminUserResponse.from(admin);
    }

    @Transactional
    public void changePassword(UUID adminUserId, ChangePasswordRequest request, String ipAddress, String userAgent) {
        AdminUser admin = adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new AdminNotFoundException("Admin user not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "New password and confirmation do not match");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setPasswordChangedAt(LocalDateTime.now());
        admin.setMustChangePassword(false);
        adminUserRepository.save(admin);

        auditLogService.logAction(
                adminUserId,
                admin.getUsername(),
                "PASSWORD_CHANGED",
                AdminAuditLog.ActionType.UPDATE,
                "ADMIN_USER",
                adminUserId.toString(),
                "Password changed",
                ipAddress,
                userAgent,
                AdminAuditLog.AuditStatus.SUCCESS,
                null
        );
    }
}
