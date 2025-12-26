package com.RRBank.banking.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration
 * Configures JWT authentication, authorization, and CORS
 * 
 * PUBLIC ENDPOINTS (no auth required):
 * - /api/auth/** (register, login, refresh, health)
 * - /actuator/health, /actuator/info
 * - Swagger UI and OpenAPI docs
 * - Static resources
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    // CORS configuration from application.yml
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOriginsString;
    
    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}")
    private String allowedMethodsString;
    
    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;
    
    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Stateless session management (no server-side sessions)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // ✅ CRITICAL: Allow OPTIONS preflight requests for CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // ✅ Public endpoints - Auth (register, login, refresh)
                        .requestMatchers("/api/auth/**").permitAll()
                        
                        // ✅ Public endpoints - Static resources
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        
                        // ✅ Public endpoints - OpenAPI / Swagger Documentation
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        
                        // ✅ Public endpoints - Basic health and info
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        
                        // 🔒 Admin only - Health details, metrics, prometheus
                        .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/metrics/**").hasRole("ADMIN")
                        
                        // 🚫 Blocked - All other actuator endpoints
                        .requestMatchers("/actuator/**").denyAll()
                        
                        // ✅ H2 Console - Only for development
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // 🔒 Protected endpoints - CUSTOMER and ADMIN roles
                        .requestMatchers("/api/accounts/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/transactions/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/transfers/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/customers/**").hasAnyRole("CUSTOMER", "ADMIN")
                        
                        // 🔒 Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // 🔒 All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // Custom authentication provider
                .authenticationProvider(authenticationProvider())
                
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    /**
     * CORS Configuration
     * 
     * Allows cross-origin requests from configured frontend origins.
     * Critical for React/Vite dev server on different port.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from comma-separated string
        List<String> origins = Arrays.asList(allowedOriginsString.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Parse allowed methods from comma-separated string
        List<String> methods = Arrays.asList(allowedMethodsString.split(","));
        configuration.setAllowedMethods(methods);
        
        // Allow all headers (or parse from config)
        if ("*".equals(allowedHeaders)) {
            configuration.setAllowedHeaders(List.of("*"));
        } else {
            configuration.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(allowCredentials);
        
        // Expose headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
