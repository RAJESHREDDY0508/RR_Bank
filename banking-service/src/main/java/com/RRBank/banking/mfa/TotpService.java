package com.RRBank.banking.mfa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

/**
 * TOTP (Time-based One-Time Password) Service
 * Implements RFC 6238 for Google Authenticator compatibility
 */
@Service
@Slf4j
public class TotpService {
    
    private static final int SECRET_SIZE = 20; // 160 bits
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP = 30; // 30 seconds
    private static final int WINDOW_SIZE = 1; // Allow 1 step before and after
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String ISSUER = "RR-Bank";
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Base32 alphabet for encoding
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    
    /**
     * Generate a new secret key for TOTP
     */
    public String generateSecret() {
        byte[] secretBytes = new byte[SECRET_SIZE];
        secureRandom.nextBytes(secretBytes);
        return encodeBase32(secretBytes);
    }
    
    /**
     * Generate QR code URL for Google Authenticator
     */
    public String generateQrCodeUrl(String secret, String username, String email) {
        String label = String.format("%s:%s", ISSUER, email);
        String encodedLabel = urlEncode(label);
        String encodedIssuer = urlEncode(ISSUER);
        
        return String.format(
            "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
            encodedLabel, secret, encodedIssuer, CODE_DIGITS, TIME_STEP
        );
    }
    
    /**
     * Generate TOTP code for current time
     */
    public String generateCode(String secret) {
        return generateCode(secret, System.currentTimeMillis() / 1000L);
    }
    
    /**
     * Generate TOTP code for specific timestamp
     */
    public String generateCode(String secret, long timeSeconds) {
        long counter = timeSeconds / TIME_STEP;
        byte[] secretBytes = decodeBase32(secret);
        
        try {
            byte[] hash = computeHmac(secretBytes, counter);
            int offset = hash[hash.length - 1] & 0xf;
            
            int binary = ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);
            
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            log.error("Error generating TOTP code", e);
            throw new RuntimeException("Failed to generate TOTP code", e);
        }
    }
    
    /**
     * Verify TOTP code with time window tolerance
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        
        long currentTimeSeconds = System.currentTimeMillis() / 1000L;
        
        // Check codes within the window
        for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
            long checkTime = currentTimeSeconds + (i * TIME_STEP);
            String generatedCode = generateCode(secret, checkTime);
            
            if (constantTimeEquals(generatedCode, code)) {
                log.debug("TOTP code verified successfully (window offset: {})", i);
                return true;
            }
        }
        
        log.debug("TOTP code verification failed");
        return false;
    }
    
    /**
     * Generate backup codes
     */
    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateBackupCode());
        }
        return codes;
    }
    
    /**
     * Generate a single backup code (8 characters)
     */
    private String generateBackupCode() {
        byte[] bytes = new byte[4];
        secureRandom.nextBytes(bytes);
        
        StringBuilder code = new StringBuilder();
        for (byte b : bytes) {
            code.append(String.format("%02x", b & 0xff));
        }
        
        // Format: XXXX-XXXX
        return code.toString().toUpperCase().substring(0, 4) + "-" + 
               code.toString().toUpperCase().substring(4, 8);
    }
    
    /**
     * Compute HMAC-SHA1
     */
    private byte[] computeHmac(byte[] key, long counter) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
        
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        return mac.doFinal(counterBytes);
    }
    
    /**
     * Encode bytes to Base32
     */
    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                result.append(BASE32_CHARS.charAt(index));
                bitsLeft -= 5;
            }
        }
        
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            result.append(BASE32_CHARS.charAt(index));
        }
        
        return result.toString();
    }
    
    /**
     * Decode Base32 to bytes
     */
    private byte[] decodeBase32(String base32) {
        String normalized = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        
        int outputLength = normalized.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        
        for (char c : normalized.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        
        return result;
    }
    
    /**
     * URL encode string
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * Constant time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
