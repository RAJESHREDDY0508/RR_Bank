package com.RRBank.banking.exception;

/**
 * Exception thrown when an account is inactive or disabled.
 * 
 * <p>This exception should be thrown when:</p>
 * <ul>
 *   <li>Account status is INACTIVE, SUSPENDED, or CLOSED</li>
 *   <li>Account requires activation</li>
 *   <li>Account has been deactivated by administrator</li>
 * </ul>
 * 
 * <p>Maps to HTTP 403 FORBIDDEN response.</p>
 * 
 * @author RR-Bank Security Team
 * @see GlobalExceptionHandler
 */
public class AccountInactiveException extends RuntimeException {
    
    private static final String DEFAULT_MESSAGE = "Account is not active";
    
    /**
     * Creates exception with default message
     */
    public AccountInactiveException() {
        super(DEFAULT_MESSAGE);
    }
    
    /**
     * Creates exception with custom message
     * 
     * @param message Custom error message
     */
    public AccountInactiveException(String message) {
        super(message);
    }
    
    /**
     * Creates exception with custom message and cause
     * 
     * @param message Custom error message
     * @param cause The underlying cause
     */
    public AccountInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
