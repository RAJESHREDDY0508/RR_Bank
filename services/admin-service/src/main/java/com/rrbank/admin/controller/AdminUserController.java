package com.rrbank.admin.controller;

import com.rrbank.admin.dto.AdminAuthDTOs.*;
import com.rrbank.admin.dto.common.ApiResponse;
import com.rrbank.admin.dto.common.PageResponse;
import com.rrbank.admin.entity.AdminAuditLog;
import com.rrbank.admin.entity.AdminUser;
import com.rrbank.admin.entity.Permission;
import com.rrbank.admin.entity.Role;
import com.rrbank.admin.exception.DuplicateResourceException;
import com.rrbank.admin.exception.ResourceNotFoundException;
import com.rrbank.admin.repository.AdminUserRepository;
import com.rrbank.admin.repository.RoleRepository;
import com.rrbank.admin.security.AdminUserDetails;
import com.rrbank.admin.security.RequirePermission;
import com.rrbank.admin.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin User Management", description = "Manage admin users")
public class AdminUserController {

    private final AdminUserRepository adminUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePermission(Permission.ADMIN_USER_READ)
    @Operation(summary = "List admin users", description = "Get paginated list of admin users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getAdminUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        AdminUser.AdminRole roleFilter = null;
        if (role != null && !role.isEmpty()) {
            try {
                roleFilter = AdminUser.AdminRole.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid role, ignore filter
            }
        }

        AdminUser.AdminStatus statusFilter = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusFilter = AdminUser.AdminStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }

        Page<AdminUser> adminUsers = adminUserRepository.findAllWithFilters(
                search,
                roleFilter,
                statusFilter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<AdminUserResponse> responses = adminUsers.getContent().stream()
                .map(AdminUserResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                responses,
                page,
                size,
                adminUsers.getTotalElements()
        )));
    }

    @GetMapping("/{id}")
    @RequirePermission(Permission.ADMIN_USER_READ)
    @Operation(summary = "Get admin user details", description = "Get detailed information about an admin user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getAdminUser(@PathVariable UUID id) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", id.toString()));

        return ResponseEntity.ok(ApiResponse.success(AdminUserResponse.from(admin)));
    }

    @PostMapping
    @RequirePermission(Permission.ADMIN_USER_MANAGE)
    @Operation(summary = "Create admin user", description = "Create a new admin user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createAdminUser(
            @Valid @RequestBody CreateAdminRequest request,
            @AuthenticationPrincipal AdminUserDetails currentAdmin,
            HttpServletRequest httpRequest
    ) {
        log.info("Creating new admin user: {} by {}", request.getUsername(), currentAdmin.getUsername());

        // Check for duplicates
        if (adminUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (adminUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }

        // Build admin user
        AdminUser admin = AdminUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(AdminUser.AdminStatus.ACTIVE)
                .department(request.getDepartment())
                .phoneNumber(request.getPhoneNumber())
                .mustChangePassword(true)
                .createdBy(currentAdmin.getId())
                .roles(new HashSet<>())
                .build();

        // Assign roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName.toUpperCase())
                        .ifPresent(roles::add);
            }
            admin.setRoles(roles);
            // Set legacy role from first role
            if (!roles.isEmpty()) {
                try {
                    admin.setRole(AdminUser.AdminRole.valueOf(roles.iterator().next().getName()));
                } catch (IllegalArgumentException e) {
                    admin.setRole(AdminUser.AdminRole.SUPPORT);
                }
            }
        } else if (request.getRole() != null) {
            // Legacy single role support
            try {
                AdminUser.AdminRole legacyRole = AdminUser.AdminRole.valueOf(request.getRole().toUpperCase());
                admin.setRole(legacyRole);
                Set<Role> adminRoles = admin.getRoles();
                roleRepository.findByName(request.getRole().toUpperCase())
                        .ifPresent(adminRoles::add);
            } catch (IllegalArgumentException e) {
                admin.setRole(AdminUser.AdminRole.SUPPORT);
            }
        } else {
            // Default to CUSTOMER_SUPPORT role
            Set<Role> adminRoles = admin.getRoles();
            roleRepository.findByName("CUSTOMER_SUPPORT")
                    .ifPresent(adminRoles::add);
            admin.setRole(AdminUser.AdminRole.CUSTOMER_SUPPORT);
        }

        AdminUser savedAdmin = adminUserRepository.save(admin);

        auditLogService.logActionSync(
                currentAdmin.getId(),
                currentAdmin.getUsername(),
                "CREATE_ADMIN_USER",
                AdminAuditLog.ActionType.CREATE,
                "ADMIN_USER",
                savedAdmin.getId().toString(),
                "Created admin user: " + savedAdmin.getUsername() + " with roles: " + savedAdmin.getRoleNames(),
                null,
                savedAdmin.getUsername(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(ApiResponse.success("Admin user created successfully", AdminUserResponse.from(savedAdmin)));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.ADMIN_USER_MANAGE)
    @Operation(summary = "Update admin user", description = "Update an admin user's details")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateAdminUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminRequest request,
            @AuthenticationPrincipal AdminUserDetails currentAdmin,
            HttpServletRequest httpRequest
    ) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", id.toString()));

        log.info("Updating admin user: {} by {}", admin.getUsername(), currentAdmin.getUsername());

        String oldValue = admin.toString();

        // Update fields
        if (request.getEmail() != null) {
            if (!admin.getEmail().equals(request.getEmail()) && adminUserRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already exists: " + request.getEmail());
            }
            admin.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) admin.setFirstName(request.getFirstName());
        if (request.getLastName() != null) admin.setLastName(request.getLastName());
        if (request.getDepartment() != null) admin.setDepartment(request.getDepartment());
        if (request.getPhoneNumber() != null) admin.setPhoneNumber(request.getPhoneNumber());

        // Update roles
        if (request.getRoles() != null) {
            Set<Role> newRoles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                roleRepository.findByName(roleName.toUpperCase())
                        .ifPresent(newRoles::add);
            }
            admin.setRoles(newRoles);
            // Update legacy role
            if (!newRoles.isEmpty()) {
                try {
                    admin.setRole(AdminUser.AdminRole.valueOf(newRoles.iterator().next().getName()));
                } catch (IllegalArgumentException e) {
                    // Keep existing
                }
            }
        } else if (request.getRole() != null) {
            // Legacy single role update
            try {
                AdminUser.AdminRole legacyRole = AdminUser.AdminRole.valueOf(request.getRole().toUpperCase());
                admin.setRole(legacyRole);
                admin.getRoles().clear();
                Set<Role> adminRoles = admin.getRoles();
                roleRepository.findByName(request.getRole().toUpperCase())
                        .ifPresent(adminRoles::add);
            } catch (IllegalArgumentException e) {
                // Ignore invalid role
            }
        }

        if (request.getStatus() != null) {
            try {
                admin.setStatus(AdminUser.AdminStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        AdminUser savedAdmin = adminUserRepository.save(admin);

        auditLogService.logActionSync(
                currentAdmin.getId(),
                currentAdmin.getUsername(),
                "UPDATE_ADMIN_USER",
                AdminAuditLog.ActionType.UPDATE,
                "ADMIN_USER",
                savedAdmin.getId().toString(),
                "Updated admin user: " + savedAdmin.getUsername(),
                oldValue,
                savedAdmin.toString(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(ApiResponse.success("Admin user updated successfully", AdminUserResponse.from(savedAdmin)));
    }

    @PutMapping("/{id}/roles")
    @RequirePermission(Permission.RBAC_MANAGE)
    @Operation(summary = "Assign roles to admin user", description = "Assign or replace roles for an admin user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> assignRoles(
            @PathVariable UUID id,
            @RequestBody AssignRolesRequest request,
            @AuthenticationPrincipal AdminUserDetails currentAdmin,
            HttpServletRequest httpRequest
    ) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", id.toString()));

        log.info("Assigning roles to admin user: {} by {}", admin.getUsername(), currentAdmin.getUsername());

        Set<String> oldRoles = admin.getRoleNames();

        // Clear and assign new roles
        admin.getRoles().clear();
        AdminUser finalAdmin = admin; // Create final reference for lambda
        for (String roleName : request.getRoleNames()) {
            roleRepository.findByName(roleName.toUpperCase())
                    .ifPresent(finalAdmin::addRole);
        }

        // Update legacy role
        if (!admin.getRoles().isEmpty()) {
            try {
                admin.setRole(AdminUser.AdminRole.valueOf(admin.getRoles().iterator().next().getName()));
            } catch (IllegalArgumentException e) {
                // Keep existing
            }
        }

        AdminUser savedAdmin = adminUserRepository.save(admin);

        auditLogService.logActionSync(
                currentAdmin.getId(),
                currentAdmin.getUsername(),
                "ASSIGN_ADMIN_ROLES",
                AdminAuditLog.ActionType.UPDATE,
                "ADMIN_USER",
                savedAdmin.getId().toString(),
                "Assigned roles to admin user: " + savedAdmin.getUsername(),
                oldRoles.toString(),
                savedAdmin.getRoleNames().toString(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(ApiResponse.success("Roles assigned successfully", AdminUserResponse.from(savedAdmin)));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.ADMIN_USER_MANAGE)
    @Operation(summary = "Deactivate admin user", description = "Deactivate an admin user (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateAdminUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal AdminUserDetails currentAdmin,
            HttpServletRequest httpRequest
    ) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", id.toString()));

        if (admin.getId().equals(currentAdmin.getId())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot deactivate your own account"));
        }

        log.info("Deactivating admin user: {} by {}", admin.getUsername(), currentAdmin.getUsername());

        admin.setStatus(AdminUser.AdminStatus.INACTIVE);
        adminUserRepository.save(admin);

        auditLogService.logActionSync(
                currentAdmin.getId(),
                currentAdmin.getUsername(),
                "DEACTIVATE_ADMIN_USER",
                AdminAuditLog.ActionType.DELETE,
                "ADMIN_USER",
                admin.getId().toString(),
                "Deactivated admin user: " + admin.getUsername(),
                "ACTIVE",
                "INACTIVE",
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(ApiResponse.success("Admin user deactivated successfully", null));
    }

    @PostMapping("/{id}/reset-password")
    @RequirePermission(Permission.ADMIN_USER_MANAGE)
    @Operation(summary = "Reset admin password", description = "Reset an admin user's password")
    public ResponseEntity<ApiResponse<String>> resetAdminPassword(
            @PathVariable UUID id,
            @AuthenticationPrincipal AdminUserDetails currentAdmin,
            HttpServletRequest httpRequest
    ) {
        AdminUser admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user", id.toString()));

        log.info("Resetting password for admin user: {} by {}", admin.getUsername(), currentAdmin.getUsername());

        // Generate temporary password
        String tempPassword = "Temp" + UUID.randomUUID().toString().substring(0, 8) + "!";
        admin.setPasswordHash(passwordEncoder.encode(tempPassword));
        admin.setMustChangePassword(true);
        adminUserRepository.save(admin);

        auditLogService.logActionSync(
                currentAdmin.getId(),
                currentAdmin.getUsername(),
                "RESET_ADMIN_PASSWORD",
                AdminAuditLog.ActionType.UPDATE,
                "ADMIN_USER",
                admin.getId().toString(),
                "Reset password for admin user: " + admin.getUsername(),
                null,
                null,
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseEntity.ok(ApiResponse.success("Password reset successful. Temporary password: " + tempPassword, tempPassword));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
