package com.RRBank.banking.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Account Number Generator
 * Generates unique account numbers
 */
@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "RR"; // Bank prefix
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate unique account number
     * Format: RR + YYYYMMDD + 8 random digits
     * Example: RR202412018374659201
     */
    public String generateAccountNumber() {
        // Get current date
        String datePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Generate 8 random digits
        String randomPart = String.format("%08d", random.nextInt(100000000));
        
        return PREFIX + datePart + randomPart;
    }

    /**
     * Generate account number with type prefix
     * Format: RR + Type + YYYYMMDD + 6 random digits
     * Example: RRSV20241201374659
     */
    public String generateAccountNumberWithType(String accountType) {
        String typePrefix = getTypePrefix(accountType);
        String datePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", random.nextInt(1000000));
        
        return PREFIX + typePrefix + datePart + randomPart;
    }

    /**
     * Get type prefix
     */
    private String getTypePrefix(String accountType) {
        return switch (accountType.toUpperCase()) {
            case "SAVINGS" -> "SV";
            case "CHECKING" -> "CK";
            case "CREDIT" -> "CR";
            case "BUSINESS" -> "BZ";
            default -> "GN"; // General
        };
    }

    /**
     * Validate account number format
     */
    public boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return false;
        }
        
        // Should start with prefix
        if (!accountNumber.startsWith(PREFIX)) {
            return false;
        }
        
        // Should have minimum length
        return accountNumber.length() >= 12;
    }
}
