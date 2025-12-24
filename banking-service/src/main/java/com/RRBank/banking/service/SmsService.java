package com.RRBank.banking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS Service
 * Handles SMS notifications via Twilio
 * 
 * NOTE: This is a mock implementation for development
 * In production, integrate with actual Twilio:
 * 1. Add Twilio SDK dependency: twilio
 * 2. Configure Twilio credentials (Account SID, Auth Token)
 * 3. Use Twilio Message.creator() API
 */
@Service
@Slf4j
public class SmsService {

    // Mock configuration - replace with actual Twilio config
    private static final String TWILIO_PHONE_NUMBER = "+1234567890";

    /**
     * Send SMS notification
     * 
     * @param toPhoneNumber recipient phone number
     * @param message SMS message content
     * @return true if sent successfully
     */
    public boolean sendSms(String toPhoneNumber, String message) {
        try {
            log.info("Sending SMS to: {}, message: {}", toPhoneNumber, message);
            
            // MOCK: Simulate SMS sending
            // In production, use Twilio:
            /*
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            
            Message twilioMessage = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(TWILIO_PHONE_NUMBER),
                message
            ).create();
            
            log.info("SMS sent successfully. SID: {}", twilioMessage.getSid());
            */
            
            // Simulate sending delay
            simulateDelay(50, 150);
            
            log.info("SMS sent successfully to: {}", toPhoneNumber);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", toPhoneNumber, e);
            return false;
        }
    }

    /**
     * Send transaction alert SMS
     */
    public boolean sendTransactionSms(String phoneNumber, String accountNumber, 
                                     String amount, String type) {
        String message = String.format(
            "RR Bank Alert: %s of $%s on account %s. If unauthorized, contact us immediately.",
            type, amount, maskAccountNumber(accountNumber)
        );
        return sendSms(phoneNumber, message);
    }

    /**
     * Send payment confirmation SMS
     */
    public boolean sendPaymentSms(String phoneNumber, String payeeName, String amount) {
        String message = String.format(
            "RR Bank: Payment of $%s to %s processed successfully.",
            amount, payeeName
        );
        return sendSms(phoneNumber, message);
    }

    /**
     * Send balance alert SMS
     */
    public boolean sendBalanceAlertSms(String phoneNumber, String accountNumber, String balance) {
        String message = String.format(
            "RR Bank Alert: Low balance of $%s on account %s. Please add funds.",
            balance, maskAccountNumber(accountNumber)
        );
        return sendSms(phoneNumber, message);
    }

    /**
     * Send OTP SMS
     */
    public boolean sendOtpSms(String phoneNumber, String otp) {
        String message = String.format(
            "RR Bank: Your verification code is %s. Valid for 10 minutes. Do not share this code.",
            otp
        );
        return sendSms(phoneNumber, message);
    }

    /**
     * Send security alert SMS
     */
    public boolean sendSecurityAlertSms(String phoneNumber, String alertMessage) {
        String message = String.format(
            "RR Bank Security Alert: %s. If this wasn't you, contact us immediately.",
            alertMessage
        );
        return sendSms(phoneNumber, message);
    }

    // ========== HELPER METHODS ==========

    /**
     * Mask account number for security
     * Example: RRSV20241201374659 -> ****374659
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 6) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 6);
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + (int) (Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
