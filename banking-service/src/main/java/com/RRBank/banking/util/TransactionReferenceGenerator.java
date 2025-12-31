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
     */
    public String generateTransactionReference() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%06d", random.nextInt(1000000));
        return PREFIX + timestamp + randomPart;
    }

    /**
     * Generate transaction reference with type prefix
     * Format: TXN + Type + YYYYMMDDHHMMSS + 4 random digits
     */
    public String generateTransactionReferenceWithType(String transactionType) {
        String typePrefix = getTypePrefix(transactionType);
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        return PREFIX + typePrefix + timestamp + randomPart;
    }

    private String getTypePrefix(String transactionType) {
        if (transactionType == null) {
            return "GEN";
        }
        return switch (transactionType.toUpperCase()) {
            case "TRANSFER" -> "TRF";
            case "TRANSFER_OUT" -> "TRO";
            case "TRANSFER_IN" -> "TRI";
            case "DEPOSIT" -> "DEP";
            case "WITHDRAWAL" -> "WTH";
            case "PAYMENT" -> "PAY";
            case "REFUND" -> "REF";
            case "FEE" -> "FEE";
            case "INTEREST" -> "INT";
            default -> "GEN";
        };
    }

    public boolean isValidTransactionReference(String reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        if (!reference.startsWith(PREFIX)) {
            return false;
        }
        return reference.length() >= 20;
    }
}
