package com.rrbank.shared.security;

/**
 * Internal service authentication header name and utilities
 */
public final class InternalAuthConstants {
    
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    public static final String SERVICE_NAME_HEADER = "X-Service-Name";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    private InternalAuthConstants() {}
    
    /**
     * Validate internal token
     */
    public static boolean isValidInternalToken(String token, String expectedSecret) {
        if (token == null || expectedSecret == null) {
            return false;
        }
        // Simple validation - in production use proper HMAC
        return token.equals(expectedSecret);
    }
}
