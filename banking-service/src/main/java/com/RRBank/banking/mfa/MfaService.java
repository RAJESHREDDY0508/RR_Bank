package com.RRBank.banking.mfa;

import com.RRBank.banking.entity.OtpCode;
import com.RRBank.banking.entity.User;
import com.RRBank.banking.entity.UserMfa;
import com.RRBank.banking.exception.MfaException;
import com.RRBank.banking.repository.OtpCodeRepository;
import com.RRBank.banking.repository.UserMfaRepository;
import com.RRBank.banking.repository.UserRepository;
import com.RRBank.banking.service.EmailService;
import com.RRBank.banking.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MFA Service - Handles all Multi-Factor Authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {
    
    private final UserMfaRepository userMfaRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${mfa.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;
    
    @Value("${mfa.otp.rate-limit-per-hour:5}")
    private int otpRateLimitPerHour;
    
    @Value("${mfa.backup-codes.count:10}")
    private int backupCodesCount;
    
    // ==================== TOTP (Google Authenticator) ====================
    
    /**
     * Setup TOTP for a user - Step 1: Generate secret
     */
    @Transactional
    public MfaSetupResponse setupTotp(String userId) {
        log.info("Setting up TOTP for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new MfaException("User not found"));
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElse(UserMfa.builder().userId(userId).build());
        
        // Generate new secret
        String secret = totpService.generateSecret();
        userMfa.setTotpSecret(secret);
        userMfa.setTotpEnabled(false);
        userMfa.setTotpVerified(false);
        
        userMfaRepository.save(userMfa);
        
        // Generate QR code URL
        String qrCodeUrl = totpService.generateQrCodeUrl(secret, user.getUsername(), user.getEmail());
        
        log.info("TOTP setup initiated for user: {}", userId);
        
        return MfaSetupResponse.builder()
            .secret(secret)
            .qrCodeUrl(qrCodeUrl)
            .method(UserMfa.MfaMethod.TOTP)
            .message("Scan the QR code with Google Authenticator and enter the verification code")
            .build();
    }
    
    /**
     * Verify and enable TOTP - Step 2: Verify code
     */
    @Transactional
    public MfaVerifyResponse verifyAndEnableTotp(String userId, String code) {
        log.info("Verifying TOTP for user: {}", userId);
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElseThrow(() -> new MfaException("MFA not setup. Please start TOTP setup first."));
        
        if (userMfa.getTotpSecret() == null) {
            throw new MfaException("TOTP not setup. Please start TOTP setup first.");
        }
        
        // Verify the code
        if (!totpService.verifyCode(userMfa.getTotpSecret(), code)) {
            userMfa.incrementMfaAttempts();
            userMfaRepository.save(userMfa);
            throw new MfaException("Invalid verification code");
        }
        
        // Enable TOTP
        userMfa.setTotpEnabled(true);
        userMfa.setTotpVerified(true);
        userMfa.setPreferredMethod(UserMfa.MfaMethod.TOTP);
        userMfa.resetMfaAttempts();
        
        // Generate backup codes
        List<String> backupCodes = totpService.generateBackupCodes(backupCodesCount);
        String hashedCodes = backupCodes.stream()
            .map(passwordEncoder::encode)
            .collect(Collectors.joining(","));
        userMfa.setBackupCodes(hashedCodes);
        userMfa.setBackupCodesGeneratedAt(LocalDateTime.now());
        
        userMfaRepository.save(userMfa);
        
        log.info("TOTP enabled successfully for user: {}", userId);
        
        return MfaVerifyResponse.builder()
            .success(true)
            .method(UserMfa.MfaMethod.TOTP)
            .backupCodes(backupCodes)
            .message("TOTP enabled successfully. Save your backup codes securely!")
            .build();
    }
    
    /**
     * Verify TOTP code for login
     */
    public boolean verifyTotp(String userId, String code) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElseThrow(() -> new MfaException("MFA not configured"));
        
        if (!userMfa.getTotpEnabled() || !userMfa.getTotpVerified()) {
            throw new MfaException("TOTP not enabled");
        }
        
        if (userMfa.isMfaLocked()) {
            throw new MfaException("MFA is temporarily locked due to too many failed attempts");
        }
        
        boolean valid = totpService.verifyCode(userMfa.getTotpSecret(), code);
        
        if (valid) {
            userMfa.resetMfaAttempts();
            userMfaRepository.save(userMfa);
        } else {
            userMfa.incrementMfaAttempts();
            userMfaRepository.save(userMfa);
        }
        
        return valid;
    }
    
    // ==================== Email OTP ====================
    
    /**
     * Setup Email OTP
     */
    @Transactional
    public MfaSetupResponse setupEmailOtp(String userId) {
        log.info("Setting up Email OTP for user: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new MfaException("User not found"));
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElse(UserMfa.builder().userId(userId).build());
        
        userMfa.setEmailEnabled(true);
        userMfa.setEmailVerified(false);
        userMfaRepository.save(userMfa);
        
        // Send verification OTP
        sendEmailOtp(userId, user.getEmail(), OtpCode.OtpPurpose.MFA_SETUP);
        
        return MfaSetupResponse.builder()
            .method(UserMfa.MfaMethod.EMAIL)
            .message("Verification code sent to your email: " + maskEmail(user.getEmail()))
            .build();
    }
    
    /**
     * Verify and enable Email OTP
     */
    @Transactional
    public MfaVerifyResponse verifyAndEnableEmailOtp(String userId, String code) {
        log.info("Verifying Email OTP for user: {}", userId);
        
        if (verifyOtp(userId, code, OtpCode.OtpType.EMAIL, OtpCode.OtpPurpose.MFA_SETUP)) {
            UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new MfaException("MFA not setup"));
            
            userMfa.setEmailVerified(true);
            if (userMfa.getPreferredMethod() == UserMfa.MfaMethod.NONE) {
                userMfa.setPreferredMethod(UserMfa.MfaMethod.EMAIL);
            }
            userMfaRepository.save(userMfa);
            
            log.info("Email OTP enabled successfully for user: {}", userId);
            
            return MfaVerifyResponse.builder()
                .success(true)
                .method(UserMfa.MfaMethod.EMAIL)
                .message("Email OTP enabled successfully")
                .build();
        }
        
        throw new MfaException("Invalid or expired verification code");
    }
    
    /**
     * Send Email OTP for login
     */
    @Transactional
    public void sendEmailOtpForLogin(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new MfaException("User not found"));
        
        sendEmailOtp(userId, user.getEmail(), OtpCode.OtpPurpose.LOGIN);
    }
    
    // ==================== SMS OTP ====================
    
    /**
     * Setup SMS OTP
     */
    @Transactional
    public MfaSetupResponse setupSmsOtp(String userId, String phoneNumber) {
        log.info("Setting up SMS OTP for user: {}", userId);
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElse(UserMfa.builder().userId(userId).build());
        
        userMfa.setSmsEnabled(true);
        userMfa.setSmsVerified(false);
        userMfa.setSmsPhoneNumber(phoneNumber);
        userMfaRepository.save(userMfa);
        
        // Send verification OTP
        sendSmsOtp(userId, phoneNumber, OtpCode.OtpPurpose.MFA_SETUP);
        
        return MfaSetupResponse.builder()
            .method(UserMfa.MfaMethod.SMS)
            .message("Verification code sent to: " + maskPhone(phoneNumber))
            .build();
    }
    
    /**
     * Verify and enable SMS OTP
     */
    @Transactional
    public MfaVerifyResponse verifyAndEnableSmsOtp(String userId, String code) {
        log.info("Verifying SMS OTP for user: {}", userId);
        
        if (verifyOtp(userId, code, OtpCode.OtpType.SMS, OtpCode.OtpPurpose.MFA_SETUP)) {
            UserMfa userMfa = userMfaRepository.findByUserId(userId)
                .orElseThrow(() -> new MfaException("MFA not setup"));
            
            userMfa.setSmsVerified(true);
            if (userMfa.getPreferredMethod() == UserMfa.MfaMethod.NONE) {
                userMfa.setPreferredMethod(UserMfa.MfaMethod.SMS);
            }
            userMfaRepository.save(userMfa);
            
            log.info("SMS OTP enabled successfully for user: {}", userId);
            
            return MfaVerifyResponse.builder()
                .success(true)
                .method(UserMfa.MfaMethod.SMS)
                .message("SMS OTP enabled successfully")
                .build();
        }
        
        throw new MfaException("Invalid or expired verification code");
    }
    
    /**
     * Send SMS OTP for login
     */
    @Transactional
    public void sendSmsOtpForLogin(String userId) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElseThrow(() -> new MfaException("MFA not configured"));
        
        if (userMfa.getSmsPhoneNumber() == null) {
            throw new MfaException("SMS not configured");
        }
        
        sendSmsOtp(userId, userMfa.getSmsPhoneNumber(), OtpCode.OtpPurpose.LOGIN);
    }
    
    // ==================== Backup Codes ====================
    
    /**
     * Verify backup code
     */
    @Transactional
    public boolean verifyBackupCode(String userId, String code) {
        log.info("Verifying backup code for user: {}", userId);
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElseThrow(() -> new MfaException("MFA not configured"));
        
        if (userMfa.getBackupCodes() == null || userMfa.getBackupCodes().isEmpty()) {
            throw new MfaException("No backup codes available");
        }
        
        String normalizedCode = code.toUpperCase().replaceAll("[^A-Z0-9]", "");
        List<String> hashedCodes = Arrays.asList(userMfa.getBackupCodes().split(","));
        
        for (int i = 0; i < hashedCodes.size(); i++) {
            if (passwordEncoder.matches(normalizedCode, hashedCodes.get(i)) ||
                passwordEncoder.matches(code, hashedCodes.get(i))) {
                // Remove used backup code
                hashedCodes.set(i, "USED");
                userMfa.setBackupCodes(String.join(",", hashedCodes));
                userMfa.resetMfaAttempts();
                userMfaRepository.save(userMfa);
                
                log.info("Backup code verified and consumed for user: {}", userId);
                return true;
            }
        }
        
        userMfa.incrementMfaAttempts();
        userMfaRepository.save(userMfa);
        return false;
    }
    
    /**
     * Regenerate backup codes
     */
    @Transactional
    public List<String> regenerateBackupCodes(String userId) {
        log.info("Regenerating backup codes for user: {}", userId);
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElseThrow(() -> new MfaException("MFA not configured"));
        
        List<String> newCodes = totpService.generateBackupCodes(backupCodesCount);
        String hashedCodes = newCodes.stream()
            .map(passwordEncoder::encode)
            .collect(Collectors.joining(","));
        
        userMfa.setBackupCodes(hashedCodes);
        userMfa.setBackupCodesGeneratedAt(LocalDateTime.now());
        userMfaRepository.save(userMfa);
        
        return newCodes;
    }
    
    // ==================== Common Operations ====================
    
    /**
     * Get MFA status for a user
     */
    public MfaStatusResponse getMfaStatus(String userId) {
        UserMfa userMfa = userMfaRepository.findByUserId(userId).orElse(null);
        
        if (userMfa == null) {
            return MfaStatusResponse.builder()
                .mfaEnabled(false)
                .totpEnabled(false)
                .smsEnabled(false)
                .emailEnabled(false)
                .preferredMethod(UserMfa.MfaMethod.NONE)
                .build();
        }
        
        int remainingBackupCodes = 0;
        if (userMfa.getBackupCodes() != null) {
            remainingBackupCodes = (int) Arrays.stream(userMfa.getBackupCodes().split(","))
                .filter(c -> !c.equals("USED"))
                .count();
        }
        
        return MfaStatusResponse.builder()
            .mfaEnabled(userMfa.isMfaEnabled())
            .totpEnabled(userMfa.getTotpEnabled() && userMfa.getTotpVerified())
            .smsEnabled(userMfa.getSmsEnabled() && userMfa.getSmsVerified())
            .emailEnabled(userMfa.getEmailEnabled() && userMfa.getEmailVerified())
            .preferredMethod(userMfa.getPreferredMethod())
            .remainingBackupCodes(remainingBackupCodes)
            .build();
    }
    
    /**
     * Disable MFA for a user
     */
    @Transactional
    public void disableMfa(String userId, String verificationCode) {
        log.info("Disabling MFA for user: {}", userId);
        
        UserMfa userMfa = userMfaRepository.findByUserId(userId)
            .orElseThrow(() -> new MfaException("MFA not configured"));
        
        // Verify current MFA before disabling
        boolean verified = false;
        
        if (userMfa.getTotpEnabled() && userMfa.getTotpVerified()) {
            verified = totpService.verifyCode(userMfa.getTotpSecret(), verificationCode);
        }
        
        if (!verified && userMfa.getBackupCodes() != null) {
            verified = verifyBackupCode(userId, verificationCode);
        }
        
        if (!verified) {
            throw new MfaException("Invalid verification code");
        }
        
        // Disable all MFA methods
        userMfa.setTotpEnabled(false);
        userMfa.setTotpVerified(false);
        userMfa.setTotpSecret(null);
        userMfa.setSmsEnabled(false);
        userMfa.setSmsVerified(false);
        userMfa.setEmailEnabled(false);
        userMfa.setEmailVerified(false);
        userMfa.setBackupCodes(null);
        userMfa.setPreferredMethod(UserMfa.MfaMethod.NONE);
        
        userMfaRepository.save(userMfa);
        
        log.info("MFA disabled successfully for user: {}", userId);
    }
    
    /**
     * Check if MFA is required for user
     */
    public boolean isMfaRequired(String userId) {
        return userMfaRepository.isMfaEnabledForUser(userId);
    }
    
    // ==================== Private Helper Methods ====================
    
    private void sendEmailOtp(String userId, String email, OtpCode.OtpPurpose purpose) {
        // Rate limiting
        checkOtpRateLimit(userId, OtpCode.OtpType.EMAIL);
        
        // Generate OTP
        String otpCode = generateOtpCode();
        
        // Save OTP
        OtpCode otp = OtpCode.builder()
            .userId(userId)
            .code(otpCode)
            .otpType(OtpCode.OtpType.EMAIL)
            .purpose(purpose)
            .destination(email)
            .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
            .build();
        
        otpCodeRepository.save(otp);
        
        // Send email
        try {
            emailService.sendOtpEmail(email, otpCode, purpose.name());
            log.info("Email OTP sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send Email OTP", e);
            throw new MfaException("Failed to send verification email");
        }
    }
    
    private void sendSmsOtp(String userId, String phoneNumber, OtpCode.OtpPurpose purpose) {
        // Rate limiting
        checkOtpRateLimit(userId, OtpCode.OtpType.SMS);
        
        // Generate OTP
        String otpCode = generateOtpCode();
        
        // Save OTP
        OtpCode otp = OtpCode.builder()
            .userId(userId)
            .code(otpCode)
            .otpType(OtpCode.OtpType.SMS)
            .purpose(purpose)
            .destination(phoneNumber)
            .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
            .build();
        
        otpCodeRepository.save(otp);
        
        // Send SMS
        try {
            smsService.sendOtpSms(phoneNumber, otpCode);
            log.info("SMS OTP sent to user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to send SMS OTP", e);
            throw new MfaException("Failed to send verification SMS");
        }
    }
    
    private boolean verifyOtp(String userId, String code, OtpCode.OtpType type, OtpCode.OtpPurpose purpose) {
        OtpCode otp = otpCodeRepository.findValidOtp(userId, type, purpose, LocalDateTime.now())
            .orElseThrow(() -> new MfaException("No valid OTP found. Please request a new code."));
        
        if (otp.isMaxAttemptsExceeded()) {
            throw new MfaException("Maximum attempts exceeded. Please request a new code.");
        }
        
        otp.incrementAttempts();
        
        if (otp.getCode().equals(code)) {
            otp.markVerified();
            otpCodeRepository.save(otp);
            return true;
        }
        
        otpCodeRepository.save(otp);
        return false;
    }
    
    private void checkOtpRateLimit(String userId, OtpCode.OtpType type) {
        long count = otpCodeRepository.countRecentOtps(userId, type, LocalDateTime.now().minusHours(1));
        if (count >= otpRateLimitPerHour) {
            throw new MfaException("Too many OTP requests. Please try again later.");
        }
    }
    
    private String generateOtpCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
    
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
    
    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
