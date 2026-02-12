package com.rrbank.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "admin_users", indexes = {
    @Index(name = "idx_admin_username", columnList = "username"),
    @Index(name = "idx_admin_email", columnList = "email")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    // Legacy role field - kept for backward compatibility
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private AdminRole role = AdminRole.SUPPORT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AdminStatus status = AdminStatus.ACTIVE;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "must_change_password")
    @Builder.Default
    private Boolean mustChangePassword = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Version
    private Long version;

    // New RBAC: Many-to-many relationship with roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "admin_user_roles",
        joinColumns = @JoinColumn(name = "admin_user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public boolean isAccountLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
            this.status = AdminStatus.LOCKED;
        }
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        if (this.status == AdminStatus.LOCKED) {
            this.status = AdminStatus.ACTIVE;
        }
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    /**
     * Get all permissions from all assigned roles
     */
    public Set<Permission> getAllPermissions() {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        return getAllPermissions().contains(permission);
    }

    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(Permission... permissions) {
        Set<Permission> userPermissions = getAllPermissions();
        for (Permission p : permissions) {
            if (userPermissions.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(Permission... permissions) {
        Set<Permission> userPermissions = getAllPermissions();
        for (Permission p : permissions) {
            if (!userPermissions.contains(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if user has a specific role by name
     */
    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
    }

    /**
     * Get role names as a set of strings
     */
    public Set<String> getRoleNames() {
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }

    /**
     * Get permission names as a set of strings
     */
    public Set<String> getPermissionNames() {
        return getAllPermissions().stream().map(Enum::name).collect(Collectors.toSet());
    }

    /**
     * Get primary role name (for backward compatibility)
     */
    public String getPrimaryRoleName() {
        // If new roles exist, use the first one
        if (!roles.isEmpty()) {
            // Prioritize SUPER_ADMIN if present
            return roles.stream()
                    .map(Role::getName)
                    .filter(name -> name.equals("SUPER_ADMIN"))
                    .findFirst()
                    .orElse(roles.iterator().next().getName());
        }
        // Fall back to legacy role
        return role != null ? role.name() : "SUPPORT";
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    // Legacy enum for backward compatibility
    public enum AdminRole {
        SUPER_ADMIN,
        SECURITY_ADMIN,
        AUDITOR,
        OPERATIONS_MANAGER,
        KYC_COMPLIANCE,
        FRAUD_ANALYST,
        CUSTOMER_SUPPORT,
        ADMIN,   // Legacy
        SUPPORT  // Legacy
    }

    public enum AdminStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        LOCKED
    }
}
