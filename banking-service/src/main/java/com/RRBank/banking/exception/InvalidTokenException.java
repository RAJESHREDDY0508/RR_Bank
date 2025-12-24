package com.RRBank.banking.exception;

/**
 * Exception thrown when a JWT token is invalid, expired, or malformed.
 * 
 * <p>This exception should be thrown when:</p>
 * <ul>
 *   <li>Token is expired</li>
 *   <li>Token signature is invalid</li>
 *   <li>Token is malformed</li>
 *   <li>Refresh token is invalid</li>
 * </ul>
 * 
 * <p>Maps to HTTP 401 UNAUTHORIZED response.</p>
 * 
 * @author RR-Bank Security Team
 * @see GlobalExceptionHandler
 */
public class InvalidTokenException extends RuntimeException {
    
    private static final String DEFAULT_MESSAGE = "Invalid or expired token";
    
    /**
     * Creates exception with default message
     */
    public InvalidTokenException() {
        super(DEFAULT_MESSAGE);
    }
    
    /**
     * Creates exception with custom message
     * 
     * @param message Custom error message
     */
    public InvalidTokenException(String message) {
        super(message);
    }
    
    /**
     * Creates exception with custom message and cause
     * 
     * @param message Custom error message
     * @param cause The underlying cause
     */
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
