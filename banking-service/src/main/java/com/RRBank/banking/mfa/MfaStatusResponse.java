package com.RRBank.banking.mfa;

import com.RRBank.banking.entity.UserMfa;
import lombok.*;

/**
 * Response DTO for MFA status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaStatusResponse {
    private boolean mfaEnabled;
    private boolean totpEnabled;
    private boolean smsEnabled;
    private boolean emailEnabled;
    private UserMfa.MfaMethod preferredMethod;
    private int remainingBackupCodes;
}
