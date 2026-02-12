package com.rrbank.admin.config;

import com.rrbank.admin.entity.AdminUser;
import com.rrbank.admin.entity.Permission;
import com.rrbank.admin.entity.Role;
import com.rrbank.admin.repository.AdminUserRepository;
import com.rrbank.admin.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Initializes RBAC roles, permissions, and default super admin user
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RbacInitializer {

    private final RoleRepository roleRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default-super-admin.username:superadmin}")
    private String defaultUsername;

    @Value("${admin.default-super-admin.password:Admin@123456}")
    private String defaultPassword;

    @Value("${admin.default-super-admin.email:admin@rrbank.com}")
    private String defaultEmail;

    @PostConstruct
    @Transactional
    public void initialize() {
        log.info("Initializing RBAC system...");
        
        // Create all system roles with permissions
        Role superAdmin = createRoleIfNotExists("SUPER_ADMIN", "Full system access", getAllPermissions());
        createRoleIfNotExists("SECURITY_ADMIN", "Security and fraud management", getSecurityAdminPermissions());
        createRoleIfNotExists("AUDITOR", "Read-only audit access", getAuditorPermissions());
        createRoleIfNotExists("OPERATIONS_MANAGER", "Operations and account management", getOperationsManagerPermissions());
        createRoleIfNotExists("KYC_COMPLIANCE", "KYC verification and compliance", getKycCompliancePermissions());
        createRoleIfNotExists("FRAUD_ANALYST", "Fraud detection and analysis", getFraudAnalystPermissions());
        createRoleIfNotExists("CUSTOMER_SUPPORT", "Customer support access", getCustomerSupportPermissions());
        
        // Create default super admin user if not exists
        createDefaultSuperAdmin(superAdmin);
        
        log.info("RBAC initialization complete.");
    }

    private Role createRoleIfNotExists(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(name)
                            .description(description)
                            .isSystemRole(true)
                            .permissions(permissions)
                            .build();
                    Role saved = roleRepository.save(role);
                    log.info("Created role: {} with {} permissions", name, permissions.size());
                    return saved;
                });
    }

    private void createDefaultSuperAdmin(Role superAdminRole) {
        if (!adminUserRepository.existsByUsername(defaultUsername)) {
            AdminUser superAdmin = AdminUser.builder()
                    .username(defaultUsername)
                    .email(defaultEmail)
                    .passwordHash(passwordEncoder.encode(defaultPassword))
                    .firstName("Super")
                    .lastName("Admin")
                    .role(AdminUser.AdminRole.SUPER_ADMIN)
                    .status(AdminUser.AdminStatus.ACTIVE)
                    .department("IT")
                    .mustChangePassword(false)
                    .roles(new HashSet<>(Arrays.asList(superAdminRole)))
                    .build();
            adminUserRepository.save(superAdmin);
            log.info("Created default super admin: {}", defaultUsername);
        } else {
            // Ensure existing super admin has the role assigned
            adminUserRepository.findByUsername(defaultUsername).ifPresent(admin -> {
                if (admin.getRoles().isEmpty()) {
                    admin.addRole(superAdminRole);
                    adminUserRepository.save(admin);
                    log.info("Assigned SUPER_ADMIN role to existing user: {}", defaultUsername);
                }
            });
        }
    }

    // Permission sets for each role
    private Set<Permission> getAllPermissions() {
        return new HashSet<>(Arrays.asList(Permission.values()));
    }

    private Set<Permission> getSecurityAdminPermissions() {
        return new HashSet<>(Arrays.asList(
                Permission.DASHBOARD_READ,
                Permission.CUSTOMER_READ,
                Permission.ACCOUNT_READ,
                Permission.TXN_READ,
                Permission.FRAUD_ALERT_READ,
                Permission.FRAUD_ALERT_MANAGE,
                Permission.FRAUD_RULES_MANAGE,
                Permission.ADMIN_USER_READ,
                Permission.ADMIN_USER_MANAGE,
                Permission.SETTINGS_READ,
                Permission.SETTINGS_MANAGE,
                Permission.AUDIT_READ
        ));
    }

    private Set<Permission> getAuditorPermissions() {
        return new HashSet<>(Arrays.asList(
                Permission.DASHBOARD_READ,
                Permission.CUSTOMER_READ,
                Permission.ACCOUNT_READ,
                Permission.TXN_READ,
                Permission.PAYMENT_READ,
                Permission.STATEMENT_READ,
                Permission.AUDIT_READ,
                Permission.ADMIN_USER_READ,
                Permission.SETTINGS_READ,
                Permission.FRAUD_ALERT_READ
        ));
    }

    private Set<Permission> getOperationsManagerPermissions() {
        return new HashSet<>(Arrays.asList(
                Permission.DASHBOARD_READ,
                Permission.CUSTOMER_READ,
                Permission.CUSTOMER_UPDATE_STATUS,
                Permission.ACCOUNT_READ,
                Permission.ACCOUNT_UPDATE_STATUS,
                Permission.ACCOUNT_APPROVE_REQUESTS,
                Permission.TXN_READ,
                Permission.TXN_EXPORT,
                Permission.STATEMENT_READ,
                Permission.STATEMENT_GENERATE,
                Permission.FRAUD_ALERT_READ,
                Permission.AUDIT_READ
        ));
    }

    private Set<Permission> getKycCompliancePermissions() {
        return new HashSet<>(Arrays.asList(
                Permission.DASHBOARD_READ,
                Permission.CUSTOMER_READ,
                Permission.CUSTOMER_KYC_UPDATE,
                Permission.CUSTOMER_UPDATE_STATUS,
                Permission.ACCOUNT_READ,
                Permission.TXN_READ,
                Permission.STATEMENT_READ,
                Permission.AUDIT_READ,
                Permission.FRAUD_ALERT_READ
        ));
    }

    private Set<Permission> getFraudAnalystPermissions() {
        return new HashSet<>(Arrays.asList(
                Permission.DASHBOARD_READ,
                Permission.CUSTOMER_READ,
                Permission.ACCOUNT_READ,
                Permission.TXN_READ,
                Permission.FRAUD_ALERT_READ,
                Permission.FRAUD_ALERT_MANAGE,
                Permission.TXN_REVERSAL_REQUEST,
                Permission.AUDIT_READ
        ));
    }

    private Set<Permission> getCustomerSupportPermissions() {
        return new HashSet<>(Arrays.asList(
                Permission.DASHBOARD_READ,
                Permission.CUSTOMER_READ,
                Permission.ACCOUNT_READ,
                Permission.TXN_READ,
                Permission.PAYMENT_READ,
                Permission.STATEMENT_READ,
                Permission.CUSTOMER_NOTES_WRITE,
                Permission.AUDIT_READ
        ));
    }
}
