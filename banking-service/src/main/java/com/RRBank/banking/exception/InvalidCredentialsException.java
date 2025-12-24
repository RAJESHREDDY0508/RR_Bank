package com.RRBank.banking.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials.
 * 
 * <p>This exception should be thrown when:</p>
 * <ul>
 *   <li>Username/email does not exist</li>
 *   <li>Password is incorrect</li>
 *   <li>Account credentials cannot be verified</li>
 * </ul>
 * 
 * <p>Maps to HTTP 401 UNAUTHORIZED response.</p>
 * 
 * <p><strong>Security Note:</strong> For security reasons, the error message
 * should be generic ("Invalid credentials") to prevent username enumeration attacks.</p>
 * 
 * @author RR-Bank Security Team
 * @see GlobalExceptionHandler
 */
public class InvalidCredentialsException extends RuntimeException {
    
    private static final String DEFAULT_MESSAGE = "Invalid credentials";
    
    /**
     * Creates exception with default message "Invalid credentials"
     */
    public InvalidCredentialsException() {
        super(DEFAULT_MESSAGE);
    }
    
    /**
     * Creates exception with custom message
     * 
     * @param message Custom error message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
    
    /**
     * Creates exception with custom message and cause
     * 
     * @param message Custom error message
     * @param cause The underlying cause
     */
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
