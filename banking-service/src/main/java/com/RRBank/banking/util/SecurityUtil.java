package com.RRBank.banking.util;

import com.RRBank.banking.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Security Utility Class
 * Provides helper methods for authentication and authorization checks
 * 
 * Phase 2A.1: Account ownership enforcement
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Extract user ID from authentication principal
     * 
     * @param auth Spring Security Authentication object
     * @return UUID of the authenticated user
     * @throws AccessDeniedException if authentication is invalid
     */
    public static UUID requireUserId(Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails cud) {
            String userId = cud.getUserId();
            if (userId == null || userId.isBlank()) {
                throw new AccessDeniedException("Invalid user ID in authentication");
            }
            try {
                return UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                throw new AccessDeniedException("Invalid user ID format");
            }
        }
        
        // Fallback for other authentication types (e.g., JWT token name)
        if (auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException e) {
                throw new AccessDeniedException("Invalid authentication principal");
            }
        }
        
        throw new AccessDeniedException("Invalid authentication principal");
    }

    /**
     * Get user ID as string from authentication principal
     * 
     * @param auth Spring Security Authentication object
     * @return User ID as string
     * @throws AccessDeniedException if authentication is invalid
     */
    public static String requireUserIdString(Authentication auth) {
        if (auth == null) {
            throw new AccessDeniedException("Authentication required");
        }
        
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails cud) {
            String userId = cud.getUserId();
            if (userId == null || userId.isBlank()) {
                throw new AccessDeniedException("Invalid user ID in authentication");
            }
            return userId;
        }
        
        if (auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        
        throw new AccessDeniedException("Invalid authentication principal");
    }

    /**
     * Check if authenticated user has ADMIN role
     * 
     * @param auth Spring Security Authentication object
     * @return true if user has ROLE_ADMIN
     */
    public static boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Check if authenticated user has CUSTOMER role
     * 
     * @param auth Spring Security Authentication object
     * @return true if user has ROLE_CUSTOMER
     */
    public static boolean isCustomer(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CUSTOMER".equals(a.getAuthority()));
    }

    /**
     * Get the username from authentication
     * 
     * @param auth Spring Security Authentication object
     * @return username or null if not available
     */
    public static String getUsername(Authentication auth) {
        if (auth == null) {
            return null;
        }
        
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails cud) {
            return cud.getUsername();
        }
        
        return auth.getName();
    }
}
