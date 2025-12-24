package com.RRBank.banking.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Payment Reference Generator
 * Generates unique payment reference numbers
 */
@Component
public class PaymentReferenceGenerator {

    private static final String PREFIX = "PAY";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate unique payment reference
     * Format: PAY + YYYYMMDDHHMMSS + 6 random digits
     * Example: PAY202412011430158374659
     */
    public String generatePaymentReference() {
        // Get current timestamp
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        
        // Generate 6 random digits
        String randomPart = String.format("%06d", random.nextInt(1000000));
        
        return PREFIX + timestamp + randomPart;
    }

    /**
     * Generate payment reference with type prefix
     * Format: PAY + Type + YYYYMMDDHHMMSS + 4 random digits
     * Example: PAYBIL20241201143015847
     */
    public String generatePaymentReferenceWithType(String paymentType) {
        String typePrefix = getTypePrefix(paymentType);
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        
        return PREFIX + typePrefix + timestamp + randomPart;
    }

    /**
     * Get type prefix
     */
    private String getTypePrefix(String paymentType) {
        return switch (paymentType.toUpperCase()) {
            case "BILL" -> "BIL";
            case "MERCHANT" -> "MER";
            case "P2P" -> "P2P";
            case "SUBSCRIPTION" -> "SUB";
            case "INVOICE" -> "INV";
            default -> "GEN"; // General
        };
    }

    /**
     * Validate payment reference format
     */
    public boolean isValidPaymentReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        
        // Should start with PAY
        if (!reference.startsWith(PREFIX)) {
            return false;
        }
        
        // Should have minimum length
        return reference.length() >= 20;
    }
}
