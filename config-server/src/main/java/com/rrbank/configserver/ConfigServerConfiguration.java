package com.rrbank.configserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Config Server Configuration
 * Enables Spring Cloud Config Server for centralized configuration management
 * 
 * This configuration starts an embedded Config Server when enabled.
 * All microservices can fetch their configuration from this server.
 * 
 * Features:
 * - Centralized configuration management
 * - Git-backed configuration (or native file system)
 * - Environment-specific configurations
 * - Configuration refresh without restart
 * - Encryption/decryption support
 * 
 * To enable:
 * - Set spring.cloud.config.server.enabled=true in application.properties
 * - Configure spring.cloud.config.server.git.uri or use native mode
 * 
 * Config Server Endpoints:
 * - http://localhost:8080/{application}/{profile}[/{label}]
 * - http://localhost:8080/{application}-{profile}.yml
 * - http://localhost:8080/{label}/{application}-{profile}.yml
 * - http://localhost:8080/{application}-{profile}.properties
 * - http://localhost:8080/{label}/{application}-{profile}.properties
 */
@Configuration
@EnableConfigServer
@ConditionalOnProperty(name = "spring.cloud.config.server.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ConfigServerConfiguration {

    @PostConstruct
    public void init() {
        log.info("===========================================");
        log.info("   CONFIG SERVER ENABLED");
        log.info("   Centralized Configuration Management");
        log.info("===========================================");
    }
}
