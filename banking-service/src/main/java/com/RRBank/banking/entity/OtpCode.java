package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing temporary OTP codes
 * Used for SMS and Email OTP verification
 */
@Entity
@Table(name = "otp_codes", indexes = {
    @Index(name = "idx_otp_user_id", columnList = "user_id"),
    @Index(name = "idx_otp_code", columnList = "code"),
    @Index(name = "idx_otp_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "otp_id", updatable = false, nullable = false)
    private String otpId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "code", nullable = false, length = 10)
    private String code;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type", nullable = false)
    private OtpType otpType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private OtpPurpose purpose;
    
    @Column(name = "destination")
    private String destination; // Phone number or email
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "verified")
    @Builder.Default
    private Boolean verified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "attempts")
    @Builder.Default
    private Integer attempts = 0;
    
    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 3;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * Check if OTP is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if max attempts exceeded
     */
    public boolean isMaxAttemptsExceeded() {
        return attempts >= maxAttempts;
    }
    
    /**
     * Check if OTP is still valid
     */
    public boolean isValid() {
        return !isExpired() && !verified && !isMaxAttemptsExceeded();
    }
    
    /**
     * Increment attempts
     */
    public void incrementAttempts() {
        this.attempts++;
    }
    
    /**
     * Mark as verified
     */
    public void markVerified() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }
    
    /**
     * OTP Type Enum
     */
    public enum OtpType {
        SMS,
        EMAIL,
        TOTP
    }
    
    /**
     * OTP Purpose Enum
     */
    public enum OtpPurpose {
        LOGIN,                  // Login verification
        MFA_SETUP,              // Setting up MFA
        MFA_VERIFICATION,       // Verifying MFA
        PASSWORD_RESET,         // Password reset
        TRANSACTION,            // High-value transaction
        PROFILE_CHANGE,         // Sensitive profile changes
        DEVICE_VERIFICATION,    // New device verification
        PHONE_VERIFICATION,     // Phone number verification
        EMAIL_VERIFICATION      // Email verification
    }
}
