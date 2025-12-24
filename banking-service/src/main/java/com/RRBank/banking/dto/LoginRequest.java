package com.RRBank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for user login requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    private String password;
}
