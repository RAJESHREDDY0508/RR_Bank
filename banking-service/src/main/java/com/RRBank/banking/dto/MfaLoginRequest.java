package com.RRBank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO for MFA verification during login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaLoginRequest {
    
    @NotBlank(message = "MFA token is required")
    private String mfaToken;
    
    @NotBlank(message = "Verification code is required")
    private String code;
    
    private String method;  // TOTP, SMS, EMAIL, BACKUP
}
