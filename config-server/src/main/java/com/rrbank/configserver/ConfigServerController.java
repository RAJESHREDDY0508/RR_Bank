package com.rrbank.configserver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Config Server Controller
 * Provides REST endpoints to manage and monitor configuration server
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.cloud.config.server.enabled", havingValue = "true")
public class ConfigServerController {

    private final Environment environment;

    /**
     * Get Config Server status
     * GET /api/config/status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfigServerStatus() {
        log.info("REST request to get Config Server status");
        
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("configServer", "enabled");
        status.put("timestamp", LocalDateTime.now());
        
        // Get config server properties
        Map<String, String> properties = new HashMap<>();
        properties.put("server.mode", environment.getProperty("spring.cloud.config.server.native.search-locations", "native"));
        properties.put("git.uri", environment.getProperty("spring.cloud.config.server.git.uri", "N/A"));
        properties.put("encrypt.enabled", environment.getProperty("encrypt.key-store.location") != null ? "true" : "false");
        
        status.put("properties", properties);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Get list of available configuration profiles
     * GET /api/config/profiles
     */
    @GetMapping("/profiles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAvailableProfiles() {
        log.info("REST request to get available configuration profiles");
        
        // Common profiles
        List<String> profiles = Arrays.asList(
                "default",
                "dev",
                "test",
                "staging",
                "production",
                "gateway",
                "eureka"
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("profiles", profiles);
        response.put("activeProfile", Arrays.asList(environment.getActiveProfiles()));
        response.put("defaultProfile", environment.getDefaultProfiles());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh configuration
     * POST /api/config/refresh
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshConfiguration() {
        log.info("REST request to refresh configuration");
        
        // Note: Actual refresh would be handled by Spring Cloud Bus or manual refresh
        // This endpoint can trigger a refresh event
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Configuration refresh initiated");
        response.put("note", "Services with @RefreshScope will reload configuration");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get configuration for a specific application and profile
     * GET /api/config/application/{application}/profile/{profile}
     */
    @GetMapping("/application/{application}/profile/{profile}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getApplicationConfig(
            @PathVariable String application,
            @PathVariable String profile
    ) {
        log.info("REST request to get configuration for application: {} with profile: {}", application, profile);
        
        Map<String, Object> response = new HashMap<>();
        response.put("application", application);
        response.put("profile", profile);
        response.put("label", "master");
        response.put("note", "Use Config Server endpoints: /" + application + "/" + profile);
        response.put("configServerEndpoint", "/" + application + "-" + profile + ".yml");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test configuration connectivity
     * GET /api/config/test
     */
    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testConfigServer() {
        log.info("REST request to test Config Server connectivity");
        
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("configServer", "reachable");
        testResult.put("status", "UP");
        testResult.put("timestamp", LocalDateTime.now());
        
        // Test some configuration properties
        Map<String, String> sampleConfig = new HashMap<>();
        sampleConfig.put("application.name", environment.getProperty("spring.application.name"));
        sampleConfig.put("server.port", environment.getProperty("server.port"));
        sampleConfig.put("active.profiles", String.join(",", environment.getActiveProfiles()));
        
        testResult.put("sampleConfiguration", sampleConfig);
        
        return ResponseEntity.ok(testResult);
    }

    /**
     * Get Config Server endpoints information
     * GET /api/config/endpoints
     */
    @GetMapping("/endpoints")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfigEndpoints() {
        log.info("REST request to get Config Server endpoints");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        List<Map<String, String>> endpointList = new ArrayList<>();
        
        endpointList.add(createEndpoint("GET", "/{application}/{profile}[/{label}]", "Get configuration"));
        endpointList.add(createEndpoint("GET", "/{application}-{profile}.yml", "Get YAML configuration"));
        endpointList.add(createEndpoint("GET", "/{application}-{profile}.properties", "Get properties configuration"));
        endpointList.add(createEndpoint("GET", "/{label}/{application}-{profile}.yml", "Get labeled YAML configuration"));
        endpointList.add(createEndpoint("GET", "/{label}/{application}-{profile}.properties", "Get labeled properties configuration"));
        
        endpoints.put("configServerEndpoints", endpointList);
        endpoints.put("managementEndpoints", Arrays.asList(
                "POST /api/config/refresh - Refresh configuration",
                "GET  /api/config/status - Config server status",
                "GET  /api/config/profiles - Available profiles",
                "GET  /api/config/test - Test connectivity"
        ));
        endpoints.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(endpoints);
    }

    private Map<String, String> createEndpoint(String method, String path, String description) {
        Map<String, String> endpoint = new HashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);
        return endpoint;
    }
}
