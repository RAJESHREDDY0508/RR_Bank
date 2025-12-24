package com.RRBank.banking.security;

import com.RRBank.banking.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT Token Provider for generating and validating JWT tokens.
 * 
 * Supports both Base64-encoded secrets and raw string secrets.
 * For HS512, the key must be at least 512 bits (64 bytes).
 */
@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;
    
    private SecretKey signingKey;
    
    private static final int HS512_MIN_KEY_SIZE_BYTES = 64;
    
    @PostConstruct
    public void init() {
        log.info("Initializing JWT Token Provider...");
        
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured!");
        }
        
        byte[] keyBytes;
        
        // Try Base64 decoding first
        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
            log.info("JWT secret decoded as Base64. Key size: {} bits ({} bytes)", 
                    keyBytes.length * 8, keyBytes.length);
        } catch (IllegalArgumentException e) {
            // Not valid Base64, use raw bytes
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            log.info("JWT secret used as raw string. Key size: {} bits ({} bytes)", 
                    keyBytes.length * 8, keyBytes.length);
        }
        
        // Check key size
        if (keyBytes.length < HS512_MIN_KEY_SIZE_BYTES) {
            log.error("JWT secret is too weak! Current: {} bytes, Required: {} bytes", 
                    keyBytes.length, HS512_MIN_KEY_SIZE_BYTES);
            log.error("Current secret value length: {} characters", jwtSecret.length());
            
            // Generate a secure key automatically for development
            log.warn("Generating a secure key for this session. UPDATE YOUR CONFIG!");
            SecretKey generatedKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
            String base64Key = Base64.getEncoder().encodeToString(generatedKey.getEncoded());
            log.warn("Add this to your configuration: jwt.secret={}", base64Key);
            
            this.signingKey = generatedKey;
        } else {
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
        
        log.info("JWT Token Provider initialized successfully!");
    }
    
    private SecretKey getSigningKey() {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing key not initialized!");
        }
        return signingKey;
    }
    
    /**
     * Generate JWT access token
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Generate JWT refresh token
     */
    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);
        
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getUserId())
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }
    
    /**
     * Get user ID from JWT token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", String.class);
    }
    
    /**
     * Get role from JWT token
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }
    
    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
    
    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get expiration time in milliseconds
     */
    public Long getExpirationTime() {
        return jwtExpiration;
    }
    
    public Long getRefreshExpirationTime() {
        return refreshExpiration;
    }
    
    /**
     * Generate a secure secret key (utility method)
     */
    public static String generateSecureSecret() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    public static void main(String[] args) {
        System.out.println("Generated JWT Secret (Base64, 512-bit):");
        System.out.println(generateSecureSecret());
    }
}
