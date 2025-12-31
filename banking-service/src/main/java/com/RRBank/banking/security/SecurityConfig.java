package com.RRBank.banking.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * - /api/auth/register, /api/auth/login, /api/auth/refresh, /api/auth/health
 * - /actuator/health, /actuator/info
 * - Swagger UI and OpenAPI docs
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Security Filter Chain...");
        
        http
                // Disable CSRF for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // ✅ Allow OPTIONS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        
                        // ✅ Public auth endpoints (specific paths, NOT /api/auth/**)
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/health").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/reset-password").permitAll()
                        
                        // 🔒 /api/auth/me requires authentication (user info endpoint)
                        .requestMatchers("/api/auth/me").authenticated()
                        
                        // ✅ Static resources
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        
                        // ✅ Swagger/OpenAPI
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        
                        // ✅ Actuator health endpoints
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        
                        // ✅ H2 Console (development only)
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // 🔒 Admin-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        
                        // 🔒 Protected endpoints - CUSTOMER and ADMIN
                        .requestMatchers("/api/accounts/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/transactions/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/transfers/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/customers/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/payments/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/statements/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/payees/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/api/disputes/**").hasAnyRole("CUSTOMER", "ADMIN")
                        
                        // 🔒 All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // Authentication provider
                .authenticationProvider(authenticationProvider())
                
                // JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        log.info("Security Filter Chain configured successfully");
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
     * CORS Configuration - Allow all origins for development
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("Configuring CORS for Banking Service...");
        
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins for development
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Correlation-ID"
        ));
        
        // Cache preflight for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS configured - All origins allowed for development");
        
        return source;
    }
}
