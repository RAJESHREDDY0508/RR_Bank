package com.RRBank.banking.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaction Reference Generator
 * Generates unique transaction reference numbers
 */
@Component
public class TransactionReferenceGenerator {

    private static final String PREFIX = "TXN";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate unique transaction reference
     * Format: TXN + YYYYMMDDHHMMSS + 6 random digits
     * Example: TXN202412011430158374659
     */
    public String generateTransactionReference() {
        // Get current timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // Generate 6 random digits
        String randomPart = String.format("%06d", random.nextInt(1000000));
        
        return PREFIX + timestamp + randomPart;
    }

    /**
     * Generate transaction reference with type prefix
     * Format: TXN + Type + YYYYMMDDHHMMSS + 4 random digits
     * Example: TXNTRF20241201143015847
     */
    public String generateTransactionReferenceWithType(String transactionType) {
        String typePrefix = getTypePrefix(transactionType);
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        
        return PREFIX + typePrefix + timestamp + randomPart;
    }

    /**
     * Get type prefix
     */
    private String getTypePrefix(String transactionType) {
        return switch (transactionType.toUpperCase()) {
            case "TRANSFER" -> "TRF";
            case "DEPOSIT" -> "DEP";
            case "WITHDRAWAL" -> "WTH";
            case "PAYMENT" -> "PAY";
            case "REFUND" -> "REF";
            case "FEE" -> "FEE";
            case "INTEREST" -> "INT";
            default -> "GEN"; // General
        };
    }

    /**
     * Validate transaction reference format
     */
    public boolean isValidTransactionReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        
        // Should start with TXN
        if (!reference.startsWith(PREFIX)) {
            return false;
        }
        
        // Should have minimum length
        return reference.length() >= 20;
    }
}
