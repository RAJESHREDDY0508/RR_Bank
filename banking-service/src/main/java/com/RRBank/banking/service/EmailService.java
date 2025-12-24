package com.RRBank.banking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email Service
 * Handles email notifications via AWS SES (Simple Email Service)
 * 
 * NOTE: This is a mock implementation for development
 * In production, integrate with actual AWS SES:
 * 1. Add AWS SDK dependency: aws-java-sdk-ses
 * 2. Configure AWS credentials
 * 3. Use AmazonSimpleEmailService client
 */
@Service
@Slf4j
public class EmailService {

    // Mock configuration - replace with actual AWS SES config
    private static final String FROM_EMAIL = "notifications@rrbank.com";
    private static final String REPLY_TO_EMAIL = "support@rrbank.com";

    /**
     * Send email notification
     * 
     * @param toEmail recipient email
     * @param subject email subject
     * @param body email body (HTML or plain text)
     * @return true if sent successfully
     */
    public boolean sendEmail(String toEmail, String subject, String body) {
        try {
            log.info("Sending email to: {}, subject: {}", toEmail, subject);
            
            // MOCK: Simulate email sending
            // In production, use AWS SES
            
            // Simulate sending delay
            simulateDelay(100, 300);
            
            log.info("Email sent successfully to: {}", toEmail);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Send HTML email
     */
    public boolean sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        return sendEmail(toEmail, subject, htmlBody);
    }

    /**
     * Send OTP email for MFA
     * 
     * @param toEmail recipient email
     * @param otpCode the OTP code to send
     * @param purpose description of what the OTP is for
     * @return true if sent successfully
     */
    public boolean sendOtpEmail(String toEmail, String otpCode, String purpose) {
        String subject = "Your RR Bank Verification Code";
        String body = buildOtpEmailBody(otpCode, purpose);
        return sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send transaction notification email
     */
    public boolean sendTransactionEmail(String toEmail, String accountNumber, 
                                       String transactionType, String amount) {
        String subject = "Transaction Alert - RR Bank";
        String body = buildTransactionEmailBody(accountNumber, transactionType, amount);
        return sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send payment notification email
     */
    public boolean sendPaymentEmail(String toEmail, String payeeName, String amount, String status) {
        String subject = "Payment Confirmation - RR Bank";
        String body = buildPaymentEmailBody(payeeName, amount, status);
        return sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send account update email
     */
    public boolean sendAccountUpdateEmail(String toEmail, String accountNumber, String updateType) {
        String subject = "Account Update - RR Bank";
        String body = buildAccountUpdateEmailBody(accountNumber, updateType);
        return sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send balance alert email
     */
    public boolean sendBalanceAlertEmail(String toEmail, String accountNumber, String balance) {
        String subject = "Low Balance Alert - RR Bank";
        String body = buildBalanceAlertEmailBody(accountNumber, balance);
        return sendHtmlEmail(toEmail, subject, body);
    }

    // ========== EMAIL TEMPLATE BUILDERS ==========

    private String buildOtpEmailBody(String otpCode, String purpose) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Verification Code</h2>
                    <p>Dear Customer,</p>
                    <p>Your verification code for <strong>%s</strong> is:</p>
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0; text-align: center;">
                        <h1 style="color: #3498db; font-size: 32px; letter-spacing: 5px; margin: 0;">%s</h1>
                    </div>
                    <p style="color: #e74c3c;"><strong>This code expires in 10 minutes.</strong></p>
                    <p>If you did not request this code, please ignore this email or contact support if you have concerns.</p>
                    <p>Best regards,<br/>RR Bank Security Team</p>
                </div>
            </body>
            </html>
            """, purpose, otpCode);
    }

    private String buildTransactionEmailBody(String accountNumber, String transactionType, String amount) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Transaction Alert</h2>
                    <p>Dear Customer,</p>
                    <p>A transaction has been processed on your account:</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Account:</strong> %s</p>
                        <p><strong>Type:</strong> %s</p>
                        <p><strong>Amount:</strong> $%s</p>
                    </div>
                    <p>If you did not authorize this transaction, please contact us immediately.</p>
                    <p>Best regards,<br/>RR Bank Team</p>
                </div>
            </body>
            </html>
            """, accountNumber, transactionType, amount);
    }

    private String buildPaymentEmailBody(String payeeName, String amount, String status) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Payment Confirmation</h2>
                    <p>Dear Customer,</p>
                    <p>Your payment has been processed:</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Payee:</strong> %s</p>
                        <p><strong>Amount:</strong> $%s</p>
                        <p><strong>Status:</strong> %s</p>
                    </div>
                    <p>Thank you for using RR Bank.</p>
                    <p>Best regards,<br/>RR Bank Team</p>
                </div>
            </body>
            </html>
            """, payeeName, amount, status);
    }

    private String buildAccountUpdateEmailBody(String accountNumber, String updateType) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Account Update</h2>
                    <p>Dear Customer,</p>
                    <p>Your account has been updated:</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p><strong>Account:</strong> %s</p>
                        <p><strong>Update:</strong> %s</p>
                    </div>
                    <p>Best regards,<br/>RR Bank Team</p>
                </div>
            </body>
            </html>
            """, accountNumber, updateType);
    }

    private String buildBalanceAlertEmailBody(String accountNumber, String balance) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #e74c3c;">Low Balance Alert</h2>
                    <p>Dear Customer,</p>
                    <p>Your account balance is low:</p>
                    <div style="background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #e74c3c;">
                        <p><strong>Account:</strong> %s</p>
                        <p><strong>Current Balance:</strong> $%s</p>
                    </div>
                    <p>Please add funds to avoid overdraft fees.</p>
                    <p>Best regards,<br/>RR Bank Team</p>
                </div>
            </body>
            </html>
            """, accountNumber, balance);
    }

    // ========== HELPER METHODS ==========

    private void simulateDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + (int) (Math.random() * (maxMs - minMs));
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
