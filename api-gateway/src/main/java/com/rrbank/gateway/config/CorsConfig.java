package com.rrbank.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:3002,http://localhost:5173,http://localhost:5174,http://localhost:5175}")
    private String allowedOriginsConfig;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Parse allowed origins from environment variable
        List<String> allowedOrigins = new ArrayList<>(Stream.of(allowedOriginsConfig.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList()));
        
        // Always include localhost ports for development
        if (!allowedOrigins.contains("http://localhost:3000")) {
            allowedOrigins.add("http://localhost:3000");
        }
        if (!allowedOrigins.contains("http://localhost:3001")) {
            allowedOrigins.add("http://localhost:3001");
        }
        if (!allowedOrigins.contains("http://localhost:3002")) {
            allowedOrigins.add("http://localhost:3002");
        }
        // Vite dev server ports (5173, 5174, 5175 - Vite increments if port is busy)
        if (!allowedOrigins.contains("http://localhost:5173")) {
            allowedOrigins.add("http://localhost:5173");
        }
        if (!allowedOrigins.contains("http://localhost:5174")) {
            allowedOrigins.add("http://localhost:5174");
        }
        if (!allowedOrigins.contains("http://localhost:5175")) {
            allowedOrigins.add("http://localhost:5175");
        }
        
        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Content-Disposition",
            "X-Total-Count",
            "X-Total-Pages"
        ));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
