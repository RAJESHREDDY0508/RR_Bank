package com.rrbank.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Gateway CORS Configuration
 * Configures Cross-Origin Resource Sharing for API Gateway
 * Allows frontend applications to communicate with the backend
 * 
 * Note: Uses reactive CORS classes for Spring WebFlux/Gateway
 */
@Configuration
@Slf4j
public class GatewayCorsConfiguration {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS for API Gateway");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins for development - restrict in production
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS",
                "HEAD"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Exposed headers (headers that the browser is allowed to access)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Identifier",
                "X-Rate-Limit-Retry-After-Seconds",
                "X-Request-Id",
                "X-Correlation-ID"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Max age of preflight request cache (in seconds)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configuration completed - All origins allowed for development");
        
        return source;
    }
}
