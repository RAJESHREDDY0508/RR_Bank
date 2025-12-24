package com.RRBank.banking.mfa;

import com.RRBank.banking.entity.UserMfa;
import lombok.*;

/**
 * Response DTO for MFA setup
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSetupResponse {
    private String secret;           // For TOTP
    private String qrCodeUrl;        // For TOTP QR code
    private UserMfa.MfaMethod method;
    private String message;
}
