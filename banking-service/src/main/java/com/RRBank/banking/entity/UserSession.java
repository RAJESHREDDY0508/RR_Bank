package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for tracking user login sessions and trusted devices
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_session_user_id", columnList = "user_id"),
    @Index(name = "idx_session_token", columnList = "session_token"),
    @Index(name = "idx_session_device_id", columnList = "device_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private String sessionId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "session_token", nullable = false, unique = true)
    private String sessionToken;
    
    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;
    
    // Device Information
    @Column(name = "device_id")
    private String deviceId;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(name = "device_type")
    private String deviceType;  // DESKTOP, MOBILE, TABLET
    
    @Column(name = "device_fingerprint")
    private String deviceFingerprint;
    
    @Column(name = "browser")
    private String browser;
    
    @Column(name = "os")
    private String os;
    
    // Location Information
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    // Session Status
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean isTrusted = false;
    
    @Column(name = "mfa_verified")
    @Builder.Default
    private Boolean mfaVerified = false;
    
    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;
    
    @Column(name = "termination_reason")
    private String terminationReason;
    
    /**
     * Check if session is valid
     */
    public boolean isValid() {
        return isActive && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
    
    /**
     * Update last activity
     */
    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
    
    /**
     * Terminate session
     */
    public void terminate(String reason) {
        this.isActive = false;
        this.terminatedAt = LocalDateTime.now();
        this.terminationReason = reason;
    }
}
