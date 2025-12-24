package com.RRBank.banking.controller;

import com.RRBank.banking.mfa.*;
import com.RRBank.banking.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for MFA operations
 * Handles TOTP, SMS, Email OTP setup and verification
 */
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {
    
    private final MfaService mfaService;
    private final JwtTokenProvider jwtTokenProvider;
    
    // ==================== MFA Status ====================
    
    /**
     * Get MFA status for current user
     * GET /api/mfa/status
     */
    @GetMapping("/status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus() {
        String userId = getCurrentUserId();
        MfaStatusResponse status = mfaService.getMfaStatus(userId);
        return ResponseEntity.ok(status);
    }
    
    // ==================== TOTP (Google Authenticator) ====================
    
    /**
     * Setup TOTP - Step 1: Get secret and QR code
     * POST /api/mfa/totp/setup
     */
    @PostMapping("/totp/setup")
    public ResponseEntity<MfaSetupResponse> setupTotp() {
        String userId = getCurrentUserId();
        log.info("TOTP setup requested for user: {}", userId);
        
        MfaSetupResponse response = mfaService.setupTotp(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify TOTP - Step 2: Verify and enable
     * POST /api/mfa/totp/verify
     */
    @PostMapping("/totp/verify")
    public ResponseEntity<MfaVerifyResponse> verifyTotp(@RequestBody @Valid TotpVerifyRequest request) {
        String userId = getCurrentUserId();
        log.info("TOTP verification requested for user: {}", userId);
        
        MfaVerifyResponse response = mfaService.verifyAndEnableTotp(userId, request.getCode());
        return ResponseEntity.ok(response);
    }
    
    // ==================== Email OTP ====================
    
    /**
     * Setup Email OTP
     * POST /api/mfa/email/setup
     */
    @PostMapping("/email/setup")
    public ResponseEntity<MfaSetupResponse> setupEmailOtp() {
        String userId = getCurrentUserId();
        log.info("Email OTP setup requested for user: {}", userId);
        
        MfaSetupResponse response = mfaService.setupEmailOtp(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify Email OTP
     * POST /api/mfa/email/verify
     */
    @PostMapping("/email/verify")
    public ResponseEntity<MfaVerifyResponse> verifyEmailOtp(@RequestBody @Valid OtpVerifyRequest request) {
        String userId = getCurrentUserId();
        log.info("Email OTP verification requested for user: {}", userId);
        
        MfaVerifyResponse response = mfaService.verifyAndEnableEmailOtp(userId, request.getCode());
        return ResponseEntity.ok(response);
    }
    
    // ==================== SMS OTP ====================
    
    /**
     * Setup SMS OTP
     * POST /api/mfa/sms/setup
     */
    @PostMapping("/sms/setup")
    public ResponseEntity<MfaSetupResponse> setupSmsOtp(@RequestBody @Valid SmsSetupRequest request) {
        String userId = getCurrentUserId();
        log.info("SMS OTP setup requested for user: {}", userId);
        
        MfaSetupResponse response = mfaService.setupSmsOtp(userId, request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify SMS OTP
     * POST /api/mfa/sms/verify
     */
    @PostMapping("/sms/verify")
    public ResponseEntity<MfaVerifyResponse> verifySmsOtp(@RequestBody @Valid OtpVerifyRequest request) {
        String userId = getCurrentUserId();
        log.info("SMS OTP verification requested for user: {}", userId);
        
        MfaVerifyResponse response = mfaService.verifyAndEnableSmsOtp(userId, request.getCode());
        return ResponseEntity.ok(response);
    }
    
    // ==================== Backup Codes ====================
    
    /**
     * Regenerate backup codes
     * POST /api/mfa/backup-codes/regenerate
     */
    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes(@RequestBody @Valid MfaVerifyRequest request) {
        String userId = getCurrentUserId();
        log.info("Backup codes regeneration requested for user: {}", userId);
        
        // Verify current MFA first
        if (!mfaService.verifyTotp(userId, request.getCode())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid verification code"));
        }
        
        List<String> newCodes = mfaService.regenerateBackupCodes(userId);
        return ResponseEntity.ok(Map.of(
            "backupCodes", newCodes,
            "message", "New backup codes generated. Save them securely!"
        ));
    }
    
    // ==================== Disable MFA ====================
    
    /**
     * Disable MFA
     * POST /api/mfa/disable
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disableMfa(@RequestBody @Valid MfaVerifyRequest request) {
        String userId = getCurrentUserId();
        log.info("MFA disable requested for user: {}", userId);
        
        mfaService.disableMfa(userId, request.getCode());
        return ResponseEntity.ok(Map.of("message", "MFA disabled successfully"));
    }
    
    // ==================== Helper Methods ====================
    
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        // The principal contains the user ID after JWT authentication
        return authentication.getName();
    }
    
    // ==================== Request DTOs ====================
    
    @lombok.Data
    public static class TotpVerifyRequest {
        @NotBlank(message = "Verification code is required")
        private String code;
    }
    
    @lombok.Data
    public static class OtpVerifyRequest {
        @NotBlank(message = "Verification code is required")
        private String code;
    }
    
    @lombok.Data
    public static class SmsSetupRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;
    }
    
    @lombok.Data
    public static class MfaVerifyRequest {
        @NotBlank(message = "Verification code is required")
        private String code;
    }
}
