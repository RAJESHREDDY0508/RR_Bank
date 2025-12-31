package com.rrbank.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

/**
 * Gateway Security Configuration
 * Configures security for reactive Spring Cloud Gateway
 * 
 * Public endpoints (no authentication required):
 * - /api/auth/** (login, register, refresh token)
 * - /actuator/** (health checks)
 * - /api/gateway/health (gateway health)
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public GatewaySecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF for stateless API
                .csrf(csrf -> csrf.disable())
                
                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // Configure authorization
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - no authentication required
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/api/auth/register").permitAll()
                        .pathMatchers("/api/auth/login").permitAll()
                        .pathMatchers("/api/auth/refresh").permitAll()
                        .pathMatchers("/api/auth/forgot-password").permitAll()
                        .pathMatchers("/api/auth/reset-password").permitAll()
                        
                        // Actuator endpoints
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        
                        // Gateway monitoring
                        .pathMatchers("/api/gateway/health").permitAll()
                        .pathMatchers("/api/gateway/routes").permitAll()
                        
                        // All other requests - pass through to downstream services
                        // The downstream service (banking-service) will handle authentication
                        .anyExchange().permitAll()
                )
                
                // Disable form login (API only)
                .formLogin(form -> form.disable())
                
                // Disable HTTP Basic
                .httpBasic(basic -> basic.disable())
                
                .build();
    }
}
