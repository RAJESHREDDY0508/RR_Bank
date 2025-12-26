package com.RRBank.banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LoginHistory Entity - Login attempt history for security auditing
 */
@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_login_user", columnList = "user_id"),
    @Index(name = "idx_login_created", columnList = "created_at"),
    @Index(name = "idx_login_ip", columnList = "ip_address"),
    @Index(name = "idx_login_success", columnList = "success")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "mfa_used")
    @Builder.Default
    private Boolean mfaUsed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static LoginHistory success(String userId, String username, String ipAddress, 
                                       String userAgent, boolean mfaUsed) {
        return LoginHistory.builder()
            .userId(userId)
            .username(username)
            .success(true)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .mfaUsed(mfaUsed)
            .build();
    }

    public static LoginHistory failure(String username, String ipAddress, 
                                       String userAgent, String reason) {
        return LoginHistory.builder()
            .username(username)
            .success(false)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .failureReason(reason)
            .build();
    }
}
