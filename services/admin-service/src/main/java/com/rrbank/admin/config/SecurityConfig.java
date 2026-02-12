package com.rrbank.admin.config;

import com.rrbank.admin.security.JwtAuthFilter;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${cors.allowed-origins:http://localhost:3001,http://localhost:3000,http://localhost:3002,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/admin/auth/login", "/api/admin/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()

                        // Admin user management - SUPER_ADMIN only
                        .requestMatchers("/api/admin/users/**").hasRole("SUPER_ADMIN")

                        // Fraud management - ADMIN and SUPER_ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/admin/fraud/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/fraud/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Account actions - ADMIN and SUPER_ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/admin/accounts/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/account-requests/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Customer status updates - ADMIN and SUPER_ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/admin/customers/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // Read-only endpoints - All admin roles
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "SUPPORT", "AUDITOR")

                        // Dashboard - All admin roles
                        .requestMatchers("/api/admin/dashboard/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "SUPPORT", "AUDITOR")

                        // Audit logs - ADMIN, SUPER_ADMIN, AUDITOR
                        .requestMatchers("/api/admin/audit-logs/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "AUDITOR")

                        // All other admin endpoints require authentication
                        .requestMatchers("/api/admin/**").authenticated()

                        // Any other request
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins
        List<String> origins = new ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        
        // Ensure all localhost ports are included for development
        if (!origins.contains("http://localhost:3000")) origins.add("http://localhost:3000");
        if (!origins.contains("http://localhost:3001")) origins.add("http://localhost:3001");
        if (!origins.contains("http://localhost:3002")) origins.add("http://localhost:3002");
        if (!origins.contains("http://localhost:5173")) origins.add("http://localhost:5173");
        
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
        return new BCryptPasswordEncoder();
    }
}
