package com.rrbank.admin.dto;

import com.rrbank.admin.entity.AdminUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdminAuthDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn;
        private AdminUserInfo user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserInfo {
        private String userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String role;  // Primary role for backward compatibility
        @Builder.Default
        private Set<String> roles = new HashSet<>();
        @Builder.Default
        private Set<String> permissions = new HashSet<>();
        private String department;
        private boolean mustChangePassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAdminRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must contain at least one uppercase, one lowercase, one number and one special character")
        private String password;

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        private String lastName;

        private String role;  // Legacy single role
        private List<String> roles;  // New: multiple roles
        private String department;
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAdminRequest {
        @Email(message = "Invalid email format")
        private String email;

        @Size(max = 50, message = "First name cannot exceed 50 characters")
        private String firstName;

        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        private String lastName;

        private String role;  // Legacy single role
        private List<String> roles;  // New: multiple roles
        private String status;
        private String department;
        private String phoneNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must contain at least one uppercase, one lowercase, one number and one special character")
        private String newPassword;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminUserResponse {
        private UUID id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private String role;  // Primary role
        @Builder.Default
        private Set<String> roles = new HashSet<>();
        @Builder.Default
        private Set<String> permissions = new HashSet<>();
        private String status;
        private String department;
        private String phoneNumber;
        private LocalDateTime lastLogin;
        private String lastLoginIp;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static AdminUserResponse from(AdminUser admin) {
            return AdminUserResponse.builder()
                    .id(admin.getId())
                    .username(admin.getUsername())
                    .email(admin.getEmail())
                    .firstName(admin.getFirstName())
                    .lastName(admin.getLastName())
                    .fullName(admin.getFullName())
                    .role(admin.getPrimaryRoleName())
                    .roles(admin.getRoleNames())
                    .permissions(admin.getPermissionNames())
                    .status(admin.getStatus().name())
                    .department(admin.getDepartment())
                    .phoneNumber(admin.getPhoneNumber())
                    .lastLogin(admin.getLastLogin())
                    .lastLoginIp(admin.getLastLoginIp())
                    .createdAt(admin.getCreatedAt())
                    .updatedAt(admin.getUpdatedAt())
                    .build();
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleResponse {
        private UUID id;
        private String name;
        private String description;
        private boolean isSystemRole;
        private Set<String> permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRolesRequest {
        private List<String> roleNames;
    }
}
