package com.RRBank.banking.exception;

/**
 * Exception thrown when attempting to create a user that already exists.
 * 
 * <p>This exception should be thrown when:</p>
 * <ul>
 *   <li>Username already exists in the system</li>
 *   <li>Email already exists in the system</li>
 *   <li>Duplicate registration attempt</li>
 * </ul>
 * 
 * <p>Maps to HTTP 409 CONFLICT response.</p>
 * 
 * @author RR-Bank Security Team
 * @see GlobalExceptionHandler
 */
public class UserAlreadyExistsException extends RuntimeException {
    
    private static final String DEFAULT_MESSAGE = "User already exists";
    
    /**
     * Creates exception with default message
     */
    public UserAlreadyExistsException() {
        super(DEFAULT_MESSAGE);
    }
    
    /**
     * Creates exception with custom message
     * 
     * @param message Custom error message
     */
    public UserAlreadyExistsException(String message) {
        super(message);
    }
    
    /**
     * Creates exception with custom message and cause
     * 
     * @param message Custom error message
     * @param cause The underlying cause
     */
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
