package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for storing user MFA settings
 * Supports TOTP (Google Authenticator), SMS, and Email OTP
 */
@Entity
@Table(name = "user_mfa", indexes = {
    @Index(name = "idx_user_mfa_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMfa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mfa_id", updatable = false, nullable = false)
    private String mfaId;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    // TOTP (Time-based One-Time Password) - Google Authenticator
    @Column(name = "totp_enabled")
    @Builder.Default
    private Boolean totpEnabled = false;
    
    @Column(name = "totp_secret")
    private String totpSecret;
    
    @Column(name = "totp_verified")
    @Builder.Default
    private Boolean totpVerified = false;
    
    // SMS OTP
    @Column(name = "sms_enabled")
    @Builder.Default
    private Boolean smsEnabled = false;
    
    @Column(name = "sms_phone_number")
    private String smsPhoneNumber;
    
    @Column(name = "sms_verified")
    @Builder.Default
    private Boolean smsVerified = false;
    
    // Email OTP
    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = false;
    
    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;
    
    // Backup codes for recovery
    @Column(name = "backup_codes", length = 1000)
    private String backupCodes;
    
    @Column(name = "backup_codes_generated_at")
    private LocalDateTime backupCodesGeneratedAt;
    
    // Preferred MFA method
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_method")
    @Builder.Default
    private MfaMethod preferredMethod = MfaMethod.NONE;
    
    // Track MFA usage
    @Column(name = "last_mfa_at")
    private LocalDateTime lastMfaAt;
    
    @Column(name = "mfa_attempts")
    @Builder.Default
    private Integer mfaAttempts = 0;
    
    @Column(name = "mfa_locked_until")
    private LocalDateTime mfaLockedUntil;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Check if any MFA method is enabled
     */
    public boolean isMfaEnabled() {
        return (totpEnabled && totpVerified) || 
               (smsEnabled && smsVerified) || 
               (emailEnabled && emailVerified);
    }
    
    /**
     * Check if MFA is locked due to too many attempts
     */
    public boolean isMfaLocked() {
        return mfaLockedUntil != null && LocalDateTime.now().isBefore(mfaLockedUntil);
    }
    
    /**
     * Increment MFA attempts and lock if necessary
     */
    public void incrementMfaAttempts() {
        this.mfaAttempts++;
        if (this.mfaAttempts >= 5) {
            this.mfaLockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }
    
    /**
     * Reset MFA attempts on successful verification
     */
    public void resetMfaAttempts() {
        this.mfaAttempts = 0;
        this.mfaLockedUntil = null;
        this.lastMfaAt = LocalDateTime.now();
    }
    
    /**
     * MFA Method Enum
     */
    public enum MfaMethod {
        NONE,
        TOTP,       // Google Authenticator / Authy
        SMS,        // SMS OTP
        EMAIL,      // Email OTP
        BACKUP      // Backup codes
    }
}
