package com.RRBank.banking.config;

import com.RRBank.banking.entity.User;
import com.RRBank.banking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Data Initializer
 * Creates default admin user on application startup if not exists
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Order(1)
    public CommandLineRunner initAdminUser() {
        return args -> {
            // Check if admin user exists
            if (!userRepository.existsByUsername("admin")) {
                log.info("Creating default admin user...");
                
                User adminUser = User.builder()
                        .userId(UUID.randomUUID().toString())
                        .username("admin")
                        .email("admin@rrbank.com")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .firstName("System")
                        .lastName("Administrator")
                        .phoneNumber("+1234567890")
                        .role(User.UserRole.ADMIN)
                        .status(User.UserStatus.ACTIVE)
                        .kycVerified(true)
                        .build();
                
                userRepository.save(adminUser);
                
                log.info("==============================================");
                log.info("DEFAULT ADMIN USER CREATED:");
                log.info("  Username: admin");
                log.info("  Password: Admin@123");
                log.info("  Email: admin@rrbank.com");
                log.info("==============================================");
            } else {
                log.info("Admin user already exists, skipping creation.");
            }
        };
    }
}
