package com.rrbank.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@rrbank.com}")
    private String fromEmail;

    @Value("${app.name:RR Bank}")
    private String appName;

    @Value("${app.frontend-url:https://rrbank.vercel.app}")
    private String frontendUrl;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a");

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("loginUrl", frontendUrl + "/login");
            context.setVariable("supportEmail", "support@rrbank.com");

            String htmlContent = templateEngine.process("welcome-email", context);
            sendHtmlEmail(toEmail, "Welcome to " + appName + "! 🎉", htmlContent);
            log.info("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendTransactionEmail(String toEmail, String firstName, String transactionType,
                                    BigDecimal amount, String accountNumber, String description,
                                    BigDecimal newBalance, String transactionRef) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("transactionType", transactionType);
            context.setVariable("amount", currencyFormat.format(amount));
            context.setVariable("accountNumber", maskAccountNumber(accountNumber));
            context.setVariable("description", description);
            context.setVariable("newBalance", currencyFormat.format(newBalance));
            context.setVariable("transactionRef", transactionRef);
            context.setVariable("transactionDate", LocalDateTime.now().format(dateFormatter));
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");
            context.setVariable("isDebit", transactionType.equalsIgnoreCase("WITHDRAWAL") || 
                                          transactionType.equalsIgnoreCase("TRANSFER_OUT"));

            String subject = getTransactionSubject(transactionType, amount);
            String htmlContent = templateEngine.process("transaction-email", context);
            sendHtmlEmail(toEmail, subject, htmlContent);
            log.info("Transaction email sent to: {} for {}", toEmail, transactionType);
        } catch (Exception e) {
            log.error("Failed to send transaction email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendTransferReceivedEmail(String toEmail, String firstName, BigDecimal amount,
                                          String fromAccount, String toAccountNumber,
                                          BigDecimal newBalance, String transactionRef) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("amount", currencyFormat.format(amount));
            context.setVariable("fromAccount", fromAccount != null ? maskAccountNumber(fromAccount) : "External Account");
            context.setVariable("toAccountNumber", maskAccountNumber(toAccountNumber));
            context.setVariable("newBalance", currencyFormat.format(newBalance));
            context.setVariable("transactionRef", transactionRef);
            context.setVariable("transactionDate", LocalDateTime.now().format(dateFormatter));
            context.setVariable("dashboardUrl", frontendUrl + "/dashboard");

            String htmlContent = templateEngine.process("transfer-received-email", context);
            sendHtmlEmail(toEmail, "💰 You received " + currencyFormat.format(amount), htmlContent);
            log.info("Transfer received email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send transfer received email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendLowBalanceAlert(String toEmail, String firstName, String accountNumber,
                                    BigDecimal currentBalance, BigDecimal threshold) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("accountNumber", maskAccountNumber(accountNumber));
            context.setVariable("currentBalance", currencyFormat.format(currentBalance));
            context.setVariable("threshold", currencyFormat.format(threshold));
            context.setVariable("depositUrl", frontendUrl + "/transfer");

            String htmlContent = templateEngine.process("low-balance-email", context);
            sendHtmlEmail(toEmail, "⚠️ Low Balance Alert - " + appName, htmlContent);
            log.info("Low balance alert sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send low balance alert to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendSecurityAlert(String toEmail, String firstName, String alertType,
                                  String deviceInfo, String ipAddress, String location) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("alertType", alertType);
            context.setVariable("deviceInfo", deviceInfo);
            context.setVariable("ipAddress", ipAddress);
            context.setVariable("location", location != null ? location : "Unknown");
            context.setVariable("timestamp", LocalDateTime.now().format(dateFormatter));
            context.setVariable("securityUrl", frontendUrl + "/settings/security");
            context.setVariable("supportEmail", "security@rrbank.com");

            String htmlContent = templateEngine.process("security-alert-email", context);
            sendHtmlEmail(toEmail, "🔐 Security Alert - " + alertType, htmlContent);
            log.info("Security alert email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send security alert to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("resetUrl", frontendUrl + "/reset-password?token=" + resetToken);
            context.setVariable("expiryTime", "1 hour");
            context.setVariable("supportEmail", "support@rrbank.com");

            String htmlContent = templateEngine.process("password-reset-email", context);
            sendHtmlEmail(toEmail, "Reset Your " + appName + " Password", htmlContent);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendAccountCreatedEmail(String toEmail, String firstName, String accountType,
                                        String accountNumber) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("accountType", accountType);
            context.setVariable("accountNumber", accountNumber);
            context.setVariable("dashboardUrl", frontendUrl + "/accounts");

            String htmlContent = templateEngine.process("account-created-email", context);
            sendHtmlEmail(toEmail, "Your new " + accountType + " account is ready! 🎊", htmlContent);
            log.info("Account created email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send account created email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendMonthlyStatementEmail(String toEmail, String firstName, String accountNumber,
                                          String month, BigDecimal openingBalance, 
                                          BigDecimal closingBalance, int transactionCount,
                                          BigDecimal totalCredits, BigDecimal totalDebits) {
        try {
            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("accountNumber", maskAccountNumber(accountNumber));
            context.setVariable("month", month);
            context.setVariable("openingBalance", currencyFormat.format(openingBalance));
            context.setVariable("closingBalance", currencyFormat.format(closingBalance));
            context.setVariable("transactionCount", transactionCount);
            context.setVariable("totalCredits", currencyFormat.format(totalCredits));
            context.setVariable("totalDebits", currencyFormat.format(totalDebits));
            context.setVariable("statementsUrl", frontendUrl + "/transactions");

            String htmlContent = templateEngine.process("monthly-statement-email", context);
            sendHtmlEmail(toEmail, "Your " + month + " Statement - " + appName, htmlContent);
            log.info("Monthly statement email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send monthly statement email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        // Show format like "****-****-1234"
        return "****-****-" + accountNumber.substring(accountNumber.length() - 4);
    }

    private String getTransactionSubject(String transactionType, BigDecimal amount) {
        String formattedAmount = currencyFormat.format(amount);
        return switch (transactionType.toUpperCase()) {
            case "DEPOSIT" -> "✅ Deposit Received: " + formattedAmount;
            case "WITHDRAWAL" -> "💸 Withdrawal: " + formattedAmount;
            case "TRANSFER", "TRANSFER_OUT" -> "↗️ Transfer Sent: " + formattedAmount;
            case "TRANSFER_IN" -> "↙️ Transfer Received: " + formattedAmount;
            default -> "Transaction Alert: " + formattedAmount;
        };
    }
}
