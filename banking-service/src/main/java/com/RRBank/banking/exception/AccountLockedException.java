package com.RRBank.banking.exception;

/**
 * Exception thrown when an account is locked due to security reasons.
 * 
 * <p>This exception should be thrown when:</p>
 * <ul>
 *   <li>Account is temporarily locked due to failed login attempts</li>
 *   <li>Account is locked by administrator</li>
 *   <li>Account requires verification before access</li>
 * </ul>
 * 
 * <p>Maps to HTTP 423 LOCKED or 401 UNAUTHORIZED response.</p>
 * 
 * @author RR-Bank Security Team
 * @see GlobalExceptionHandler
 */
public class AccountLockedException extends RuntimeException {
    
    private static final String DEFAULT_MESSAGE = "Account is temporarily locked. Please try again later.";
    
    /**
     * Creates exception with default message
     */
    public AccountLockedException() {
        super(DEFAULT_MESSAGE);
    }
    
    /**
     * Creates exception with custom message
     * 
     * @param message Custom error message
     */
    public AccountLockedException(String message) {
        super(message);
    }
    
    /**
     * Creates exception with custom message and cause
     * 
     * @param message Custom error message
     * @param cause The underlying cause
     */
    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
