package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized Error Response DTO
 * Ensures consistent error format across all API endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    /**
     * HTTP status code (e.g., 400, 404, 500)
     */
    private int code;
    
    /**
     * Error type/category (e.g., "VALIDATION_ERROR", "NOT_FOUND", "UNAUTHORIZED")
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Additional error details (optional)
     * Used for validation errors or multiple error messages
     */
    private List<String> details;
    
    /**
     * Timestamp when the error occurred
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Request path where the error occurred
     */
    private String path;
    
    /**
     * Constructor for simple errors (no details)
     */
    public ErrorResponse(int code, String error, String message, String path) {
        this.code = code;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.details = null;
    }
    
    /**
     * Constructor for detailed errors (with validation details)
     */
    public ErrorResponse(int code, String error, String message, List<String> details, String path) {
        this.code = code;
        this.error = error;
        this.message = message;
        this.details = details;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }
}
