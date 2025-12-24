package com.RRBank.banking.dto;

import com.RRBank.banking.entity.UserMfa;
import lombok.*;

/**
 * DTO for authentication response with MFA support
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    
    // User info
    private String userId;
    private String username;
    private String email;
    private String role;
    
    // MFA fields
    private boolean mfaRequired;
    private String mfaToken;  // Temporary token for MFA verification
    private UserMfa.MfaMethod[] availableMfaMethods;
    private UserMfa.MfaMethod preferredMfaMethod;
    
    // Status
    private boolean success;
    private String message;
}
