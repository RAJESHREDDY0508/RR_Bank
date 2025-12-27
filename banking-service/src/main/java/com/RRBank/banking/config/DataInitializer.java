package com.RRBank.banking.config;

import com.RRBank.banking.entity.User;
import com.RRBank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Data Initializer - Seeds initial admin account for development
 * 
 * Runs on ALL profiles (removed @Profile restriction).
 * For production, admin accounts should be created through secure processes.
 */
@Component
@Order(1)  // Run early
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Default admin credentials (CHANGE IN PRODUCTION!)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@rrbank.com";
    private static final String ADMIN_PASSWORD = "Admin@123!";

    @Override
    public void run(String... args) {
        log.info("==============================================");
        log.info("  DataInitializer STARTING...");
        log.info("==============================================");
        
        try {
            createAdminUserIfNotExists();
        } catch (Exception e) {
            log.error("DataInitializer FAILED: {}", e.getMessage(), e);
        }
        
        log.info("==============================================");
        log.info("  DataInitializer COMPLETED");
        log.info("==============================================");
    }

    private void createAdminUserIfNotExists() {
        log.info("Checking for existing admin user...");
        log.info("Total users in database: {}", userRepository.count());
        
        // Check if admin already exists by username
        boolean existsByUsername = userRepository.existsByUsername(ADMIN_USERNAME);
        boolean existsByEmail = userRepository.existsByEmail(ADMIN_EMAIL);
        
        log.info("Exists by username '{}': {}", ADMIN_USERNAME, existsByUsername);
        log.info("Exists by email '{}': {}", ADMIN_EMAIL, existsByEmail);
        
        if (existsByUsername) {
            log.info("Admin user '{}' already exists", ADMIN_USERNAME);
            
            // Verify password works by loading and checking
            userRepository.findByUsername(ADMIN_USERNAME).ifPresent(admin -> {
                log.info("=== EXISTING ADMIN USER DETAILS ===");
                log.info("User ID: {}", admin.getUserId());
                log.info("Username: {}", admin.getUsername());
                log.info("Email: {}", admin.getEmail());
                log.info("Role: {}", admin.getRole());
                log.info("Status: {}", admin.getStatus());
                log.info("Account Locked: {}", admin.isAccountLocked());
                log.info("Failed Login Attempts: {}", admin.getFailedLoginAttempts());
                
                // Test password encoding
                boolean passwordMatches = passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash());
                log.info("Password verification test (expected: Admin@123!): {}", 
                        passwordMatches ? "✅ PASSED" : "❌ FAILED");
                
                if (!passwordMatches) {
                    log.warn("Password doesn't match! Resetting admin password...");
                    admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                    admin.setFailedLoginAttempts(0);
                    admin.setAccountLockedUntil(null);
                    admin.setStatus(User.UserStatus.ACTIVE);
                    userRepository.save(admin);
                    log.info("✅ Admin password reset successfully");
                    
                    // Verify again
                    boolean verifyAgain = passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash());
                    log.info("Password verification after reset: {}", verifyAgain ? "✅ PASSED" : "❌ FAILED");
                }
                
                // Ensure account is unlocked and active
                if (admin.isAccountLocked() || admin.getStatus() != User.UserStatus.ACTIVE) {
                    log.warn("Admin account is locked or inactive. Unlocking...");
                    admin.setAccountLockedUntil(null);
                    admin.setFailedLoginAttempts(0);
                    admin.setStatus(User.UserStatus.ACTIVE);
                    userRepository.save(admin);
                    log.info("✅ Admin account unlocked and activated");
                }
            });
            return;
        }

        if (existsByEmail) {
            log.info("Admin email '{}' already exists with different username, skipping", ADMIN_EMAIL);
            return;
        }

        // Create admin user
        log.info("Creating new admin user...");
        
        String encodedPassword = passwordEncoder.encode(ADMIN_PASSWORD);
        log.info("Password encoded successfully (length: {})", encodedPassword.length());
        
        User admin = User.builder()
                .userId(UUID.randomUUID().toString())  // Explicitly set UUID
                .username(ADMIN_USERNAME)
                .email(ADMIN_EMAIL)
                .passwordHash(encodedPassword)
                .firstName("System")
                .lastName("Administrator")
                .role(User.UserRole.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .kycVerified(true)
                .failedLoginAttempts(0)
                .build();

        admin = userRepository.save(admin);

        log.info("==============================================");
        log.info("  ✅ DEFAULT ADMIN ACCOUNT CREATED");
        log.info("==============================================");
        log.info("  User ID:  {}", admin.getUserId());
        log.info("  Username: {}", ADMIN_USERNAME);
        log.info("  Email:    {}", ADMIN_EMAIL);
        log.info("  Password: {}", ADMIN_PASSWORD);
        log.info("  Role:     {}", admin.getRole());
        log.info("  Status:   {}", admin.getStatus());
        log.info("==============================================");
        log.warn("  ⚠️  CHANGE THIS PASSWORD IN PRODUCTION!");
        log.info("==============================================");
        
        // Verify the saved user
        boolean verifyPassword = passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash());
        log.info("Password verification after save: {}", verifyPassword ? "✅ PASSED" : "❌ FAILED");
    }
}
