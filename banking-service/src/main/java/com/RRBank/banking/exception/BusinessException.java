package com.RRBank.banking.exception;

/**
 * Business Exception
 * Thrown when business rules are violated
 */
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
