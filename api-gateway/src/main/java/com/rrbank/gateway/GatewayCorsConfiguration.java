package com.RRBank.banking.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Gateway CORS Configuration
 * Configures Cross-Origin Resource Sharing for API Gateway
 * Allows frontend applications to communicate with the backend
 */
@Configuration
@Slf4j
public class GatewayCorsConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS for API Gateway");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (frontend applications)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",      // React dev server
                "http://localhost:5173",      // Vite dev server
                "http://localhost:8080",      // Same origin
                "http://localhost:4200",      // Angular dev server
                "https://rrbank.com",         // Production domain
                "https://www.rrbank.com"      // Production domain with www
        ));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Identifier"
        ));
        
        // Exposed headers (headers that the browser is allowed to access)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Identifier",
                "X-Rate-Limit-Retry-After-Seconds",
                "X-Request-Id"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Max age of preflight request cache (in seconds)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configuration completed - Allowed origins: {}", configuration.getAllowedOrigins());
        
        return source;
    }
}
