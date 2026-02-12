package com.rrbank.admin.controller;

import com.rrbank.admin.dto.AdminAuthDTOs.*;
import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.security.AdminUserDetails;
import com.rrbank.admin.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Authentication", description = "Admin authentication and session management")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(summary = "Admin login", description = "Authenticate admin user and get JWT tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Admin login attempt for user: {} from IP: {}", request.getUsername(), ipAddress);
        log.debug("Request headers - User-Agent: {}, Origin: {}", userAgent, httpRequest.getHeader("Origin"));
        
        try {
            LoginResponse response = adminAuthService.login(request, ipAddress, userAgent);
            log.info("Login successful for user: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (Exception e) {
            log.error("Login failed for user: {} - Error: {}", request.getUsername(), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Get new access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestHeader("Authorization") String authHeader
    ) {
        String refreshToken = authHeader.replace("Bearer ", "");
        LoginResponse response = adminAuthService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout and invalidate all tokens")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal AdminUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        adminAuthService.logout(userDetails.getId(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current admin", description = "Get current authenticated admin user info")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getCurrentAdmin(
            @AuthenticationPrincipal AdminUserDetails userDetails
    ) {
        AdminUserResponse response = adminAuthService.getCurrentAdmin(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change current admin's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal AdminUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        adminAuthService.changePassword(userDetails.getId(), request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
