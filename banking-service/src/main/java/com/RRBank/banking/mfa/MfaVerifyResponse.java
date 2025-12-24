package com.RRBank.banking.mfa;

import com.RRBank.banking.entity.UserMfa;
import lombok.*;

import java.util.List;

/**
 * Response DTO for MFA verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaVerifyResponse {
    private boolean success;
    private UserMfa.MfaMethod method;
    private List<String> backupCodes;  // Only returned when setting up TOTP
    private String message;
}
