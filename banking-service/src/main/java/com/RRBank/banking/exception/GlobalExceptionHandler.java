package com.RRBank.banking.exception;

import com.RRBank.banking.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Exception Handler
 * 
 * <p>Provides centralized exception handling across all REST controllers.
 * Ensures consistent error response format for all API endpoints.</p>
 * 
 * <h2>Response Format:</h2>
 * <pre>
 * {
 *   "code": 401,
 *   "error": "INVALID_CREDENTIALS",
 *   "message": "Invalid credentials",
 *   "details": null,
 *   "timestamp": "2024-01-15T10:30:00",
 *   "path": "/api/auth/login"
 * }
 * </pre>
 * 
 * <h2>HTTP Status Code Mapping:</h2>
 * <ul>
 *   <li>400 - Validation errors, bad requests</li>
 *   <li>401 - Authentication failures (invalid credentials, expired tokens)</li>
 *   <li>403 - Authorization failures (access denied, inactive accounts)</li>
 *   <li>404 - Resource not found</li>
 *   <li>409 - Conflicts (duplicate resources)</li>
 *   <li>423 - Account locked</li>
 *   <li>500 - Unexpected server errors</li>
 * </ul>
 * 
 * @author RR-Bank Development Team
 * @version 2.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== AUTHENTICATION EXCEPTIONS (401) ====================

    /**
     * Handle invalid credentials exception.
     * Returns HTTP 401 UNAUTHORIZED.
     * 
     * <p><strong>Security Note:</strong> Uses generic message to prevent
     * username enumeration attacks.</p>
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
            InvalidCredentialsException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Invalid credentials attempt from IP: {}, path: {}", 
                getClientIP(request), request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle invalid/expired token exception.
     * Returns HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(
            InvalidTokenException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_TOKEN",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Invalid token: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle Spring Security authentication exceptions.
     * Returns HTTP 401 UNAUTHORIZED.
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "AUTHENTICATION_FAILED",
                "Authentication failed",
                request.getRequestURI()
        );
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // ==================== AUTHORIZATION EXCEPTIONS (403) ====================

    /**
     * Handle access denied errors (authorization failures).
     * Returns HTTP 403 FORBIDDEN.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "You do not have permission to access this resource",
                request.getRequestURI()
        );
        
        log.warn("Access denied for path: {}", request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle inactive account exception.
     * Returns HTTP 403 FORBIDDEN.
     */
    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountInactiveException(
            AccountInactiveException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "ACCOUNT_INACTIVE",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Inactive account access attempt: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handle Spring Security DisabledException.
     * Returns HTTP 403 FORBIDDEN.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "ACCOUNT_DISABLED",
                "Account is disabled",
                request.getRequestURI()
        );
        
        log.warn("Disabled account access attempt");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ==================== ACCOUNT LOCKED (423) ====================

    /**
     * Handle account locked exception.
     * Returns HTTP 423 LOCKED.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLockedException(
            AccountLockedException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.LOCKED.value(),
                "ACCOUNT_LOCKED",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Locked account access attempt from IP: {}", getClientIP(request));
        
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    /**
     * Handle Spring Security LockedException.
     * Returns HTTP 423 LOCKED.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleSpringLockedException(
            LockedException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.LOCKED.value(),
                "ACCOUNT_LOCKED",
                "Account is temporarily locked. Please try again later.",
                request.getRequestURI()
        );
        
        log.warn("Spring Security locked account");
        
        return ResponseEntity.status(HttpStatus.LOCKED).body(error);
    }

    // ==================== CONFLICT EXCEPTIONS (409) ====================

    /**
     * Handle user already exists exception.
     * Returns HTTP 409 CONFLICT.
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "USER_ALREADY_EXISTS",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Duplicate user registration attempt: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==================== NOT FOUND EXCEPTIONS (404) ====================

    /**
     * Handle resource not found errors.
     * Returns HTTP 404 NOT FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle 404 - endpoint not found.
     * Returns HTTP 404 NOT FOUND.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "ENDPOINT_NOT_FOUND",
                String.format("Endpoint '%s %s' not found", ex.getHttpMethod(), ex.getRequestURL()),
                request.getRequestURI()
        );
        
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ==================== BAD REQUEST EXCEPTIONS (400) ====================

    /**
     * Handle malformed JSON / JSON parsing errors.
     * Returns HTTP 400 BAD REQUEST.
     * 
     * Common causes:
     * - Invalid JSON syntax
     * - Type mismatch (e.g., String instead of Object)
     * - Missing required fields
     * - Invalid date format
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        String message = "Invalid JSON request body";
        List<String> details = new ArrayList<>();
        
        Throwable cause = ex.getCause();
        if (cause instanceof JsonParseException) {
            message = "Malformed JSON syntax";
            details.add("Check for missing commas, brackets, or quotes");
        } else if (cause instanceof JsonMappingException jsonEx) {
            message = "JSON type mismatch or invalid value";
            // Extract field path from JsonMappingException
            StringBuilder fieldPath = new StringBuilder();
            for (JsonMappingException.Reference ref : jsonEx.getPath()) {
                if (fieldPath.length() > 0) fieldPath.append(".");
                if (ref.getFieldName() != null) {
                    fieldPath.append(ref.getFieldName());
                } else {
                    fieldPath.append("[").append(ref.getIndex()).append("]");
                }
            }
            if (fieldPath.length() > 0) {
                details.add("Field: " + fieldPath);
            }
            // Add original message for context
            String originalMessage = jsonEx.getOriginalMessage();
            if (originalMessage != null && !originalMessage.isEmpty()) {
                details.add(originalMessage);
            }
        } else if (ex.getMessage() != null) {
            // Generic message extraction
            if (ex.getMessage().contains("Required request body is missing")) {
                message = "Request body is required";
            } else if (ex.getMessage().contains("Cannot deserialize")) {
                message = "Invalid data type in request";
                details.add("Check that all fields have correct types (e.g., dates as \"yyyy-MM-dd\")");
            }
        }
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_JSON",
                message,
                details.isEmpty() ? null : details,
                request.getRequestURI()
        );
        
        log.warn("JSON parsing error at {}: {}", request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle validation errors (e.g., @Valid annotation failures).
     * Returns HTTP 400 BAD REQUEST with field-level details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<String> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed for one or more fields",
                details,
                request.getRequestURI()
        );
        
        log.warn("Validation error: {}", details);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle business logic errors.
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "BUSINESS_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Business error: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle insufficient funds errors.
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INSUFFICIENT_FUNDS",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Insufficient funds: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle illegal argument errors.
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Invalid argument: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle illegal state errors.
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_STATE",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        log.warn("Invalid state: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle type mismatch errors.
     * Returns HTTP 400 BAD REQUEST.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "TYPE_MISMATCH",
                message,
                request.getRequestURI()
        );
        
        log.warn("Type mismatch: {}", message);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ==================== INTERNAL SERVER ERROR (500) ====================

    /**
     * Handle all other unexpected exceptions.
     * Returns HTTP 500 INTERNAL SERVER ERROR.
     * 
     * <p><strong>Security Note:</strong> Does not expose internal error details
     * to prevent information leakage.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        
        // Log full stack trace for debugging (not exposed to client)
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Extract client IP address from request.
     * Handles proxy headers (X-Forwarded-For).
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
