package com.RRBank.banking.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Utility class for generating secure JWT secrets.
 * 
 * <h2>Usage:</h2>
 * <pre>
 * // Run from command line:
 * java -cp banking-service.jar com.RRBank.banking.util.JwtSecretGenerator
 * 
 * // Or programmatically:
 * String secret = JwtSecretGenerator.generateHS512Secret();
 * </pre>
 * 
 * <h2>Security Requirements (RFC 7518):</h2>
 * <table border="1">
 *   <tr><th>Algorithm</th><th>Minimum Key Size</th><th>Bytes</th></tr>
 *   <tr><td>HS256</td><td>256 bits</td><td>32 bytes</td></tr>
 *   <tr><td>HS384</td><td>384 bits</td><td>48 bytes</td></tr>
 *   <tr><td>HS512</td><td>512 bits</td><td>64 bytes</td></tr>
 * </table>
 * 
 * @author RR-Bank Security Team
 * @see <a href="https://tools.ietf.org/html/rfc7518#section-3.2">RFC 7518 Section 3.2</a>
 */
public class JwtSecretGenerator {
    
    /**
     * Generate a cryptographically secure secret for HS512 algorithm.
     * 
     * @return Base64-encoded 512-bit (64 byte) secret key
     */
    public static String generateHS512Secret() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Generate a cryptographically secure secret for HS384 algorithm.
     * 
     * @return Base64-encoded 384-bit (48 byte) secret key
     */
    public static String generateHS384Secret() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS384);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Generate a cryptographically secure secret for HS256 algorithm.
     * 
     * @return Base64-encoded 256-bit (32 byte) secret key
     */
    public static String generateHS256Secret() {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Validate that a Base64-encoded secret meets minimum size requirements.
     * 
     * @param base64Secret The Base64-encoded secret to validate
     * @param algorithm The signature algorithm to validate against
     * @return true if the secret meets minimum requirements
     */
    public static boolean isValidKeySize(String base64Secret, SignatureAlgorithm algorithm) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
            int keySizeBits = keyBytes.length * 8;
            
            return switch (algorithm) {
                case HS256 -> keySizeBits >= 256;
                case HS384 -> keySizeBits >= 384;
                case HS512 -> keySizeBits >= 512;
                default -> false;
            };
        } catch (IllegalArgumentException e) {
            return false; // Invalid Base64
        }
    }
    
    /**
     * Get key size information for a Base64-encoded secret.
     * 
     * @param base64Secret The Base64-encoded secret
     * @return Human-readable key size information
     */
    public static String getKeyInfo(String base64Secret) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
            int bits = keyBytes.length * 8;
            return String.format("%d bits (%d bytes)", bits, keyBytes.length);
        } catch (IllegalArgumentException e) {
            return "Invalid Base64 encoding";
        }
    }
    
    /**
     * Main method - run to generate a new secure JWT secret.
     */
    public static void main(String[] args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         RR-Bank JWT Secret Key Generator                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Generate secrets for all HMAC algorithms
        String hs512 = generateHS512Secret();
        String hs384 = generateHS384Secret();
        String hs256 = generateHS256Secret();
        
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ HS512 (Recommended for RR-Bank)                                      │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│ Key Size: " + getKeyInfo(hs512));
        System.out.println("│ Secret:   " + hs512);
        System.out.println("└──────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ HS384                                                                │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│ Key Size: " + getKeyInfo(hs384));
        System.out.println("│ Secret:   " + hs384);
        System.out.println("└──────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ HS256                                                                │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│ Key Size: " + getKeyInfo(hs256));
        System.out.println("│ Secret:   " + hs256);
        System.out.println("└──────────────────────────────────────────────────────────────────────┘");
        System.out.println();
        
        System.out.println("═══════════════════════════════════════════════════════════════════════");
        System.out.println(" USAGE INSTRUCTIONS:");
        System.out.println("═══════════════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println(" 1. Copy the HS512 secret above");
        System.out.println();
        System.out.println(" 2. Add to application.properties:");
        System.out.println("    jwt.secret=" + hs512);
        System.out.println();
        System.out.println(" 3. Or set as environment variable:");
        System.out.println("    export JWT_SECRET=\"" + hs512 + "\"");
        System.out.println();
        System.out.println(" 4. Or add to .env file:");
        System.out.println("    JWT_SECRET=" + hs512);
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════");
        System.out.println(" ⚠️  SECURITY WARNINGS:");
        System.out.println("═══════════════════════════════════════════════════════════════════════");
        System.out.println(" • NEVER commit secrets to version control");
        System.out.println(" • Use different secrets for dev/staging/production");
        System.out.println(" • Rotate secrets periodically");
        System.out.println(" • Store production secrets in a secrets manager");
        System.out.println("═══════════════════════════════════════════════════════════════════════");
        System.out.println();
    }
}
