package com.RRBank.banking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User Entity - Authentication and user management
 * 
 * IMPORTANT: This entity maps to the 'users' table with user_id as VARCHAR(36)
 * The ID is generated as UUID string, NOT native PostgreSQL UUID type.
 * 
 * Database constraints:
 * - role: CUSTOMER, ADMIN, TELLER, MANAGER
 * - status: ACTIVE, INACTIVE, SUSPENDED, LOCKED
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_role", columnList = "role"),
    @Index(name = "idx_users_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @Column(name = "user_id", length = 36, updatable = false, nullable = false)
    private String userId;
    
    @Column(name = "username", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;
    
    @Column(name = "email", unique = true, nullable = false, length = 100)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    @NotBlank(message = "Password is required")
    private String passwordHash;
    
    @Column(name = "first_name", nullable = false, length = 50)
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 50)
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    // Phone number is optional - empty string or valid phone format allowed
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "address", length = 255)
    private String address;
    
    @Column(name = "city", length = 50)
    private String city;
    
    @Column(name = "state", length = 50)
    private String state;
    
    @Column(name = "postal_code", length = 10)
    private String postalCode;
    
    @Column(name = "country", length = 50)
    private String country;
    
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(name = "kyc_verified")
    @Builder.Default
    private Boolean kycVerified = false;
    
    @Column(name = "kyc_verification_date")
    private LocalDateTime kycVerificationDate;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();
    
    /**
     * Generate UUID before persist if not set
     */
    @PrePersist
    protected void onCreate() {
        if (userId == null || userId.isBlank()) {
            userId = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * Get full name of user
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Check if account is currently locked
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && LocalDateTime.now().isBefore(accountLockedUntil);
    }
    
    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
    
    /**
     * Check if user is active
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
    
    /**
     * User roles - MUST match database constraint
     * Database: CHECK (role IN ('CUSTOMER', 'ADMIN', 'TELLER', 'MANAGER'))
     */
    public enum UserRole {
        CUSTOMER,   // Regular bank customer
        ADMIN,      // System administrator
        TELLER,     // Bank teller
        MANAGER     // Branch manager
    }
    
    /**
     * User status - MUST match database constraint
     * Database: CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED'))
     */
    public enum UserStatus {
        ACTIVE,     // User is active and can login
        INACTIVE,   // User is inactive (self-deactivated)
        SUSPENDED,  // User is suspended by admin
        LOCKED      // User is locked due to failed attempts
    }
}
