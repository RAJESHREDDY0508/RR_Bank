package com.RRBank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for user registration requests
 * 
 * Field requirements:
 * - username: 3-50 chars, required
 * - email: valid email format, required
 * - password: 8+ chars, must contain digit, lowercase, uppercase, special char
 * - firstName: required
 * - lastName: required
 * - phoneNumber: optional, if provided must be 10-15 digits (can start with +)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*+=?_\\-]).*$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character (!@#$%^&*+=?_-)"
    )
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    // Phone number is optional - if provided, must be valid format
    @Pattern(regexp = "^$|^[+]?[0-9]{10,15}$", message = "Invalid phone number format (10-15 digits, may start with +)")
    private String phoneNumber;
    
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
