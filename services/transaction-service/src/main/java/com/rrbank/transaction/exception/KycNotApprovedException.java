package com.rrbank.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user attempts a transaction but their KYC is not approved.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class KycNotApprovedException extends RuntimeException {
    
    private final String errorCode;
    private final String kycStatus;
    
    public KycNotApprovedException(String kycStatus) {
        super("Your account is pending KYC verification. Transactions will be enabled once admin approves your KYC.");
        this.errorCode = "KYC_REQUIRED";
        this.kycStatus = kycStatus;
    }
    
    public KycNotApprovedException(String kycStatus, String message) {
        super(message);
        this.errorCode = "KYC_REQUIRED";
        this.kycStatus = kycStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getKycStatus() {
        return kycStatus;
    }
}
