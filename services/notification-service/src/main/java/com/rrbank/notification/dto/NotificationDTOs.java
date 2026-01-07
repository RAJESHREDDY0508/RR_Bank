package com.rrbank.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class NotificationDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WelcomeEmailRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionEmailRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotBlank
        private String transactionType;
        
        @NotNull
        private BigDecimal amount;
        
        @NotBlank
        private String accountNumber;
        
        private String description;
        
        @NotNull
        private BigDecimal newBalance;
        
        @NotBlank
        private String transactionRef;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferReceivedEmailRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotNull
        private BigDecimal amount;
        
        private String fromAccount;
        
        @NotBlank
        private String toAccountNumber;
        
        @NotNull
        private BigDecimal newBalance;
        
        @NotBlank
        private String transactionRef;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityAlertRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotBlank
        private String alertType;
        
        private String deviceInfo;
        
        private String ipAddress;
        
        private String location;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasswordResetEmailRequest {
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotBlank
        private String resetToken;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCreatedEmailRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotBlank
        private String accountType;
        
        @NotBlank
        private String accountNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowBalanceAlertRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotBlank
        private String accountNumber;
        
        @NotNull
        private BigDecimal currentBalance;
        
        @NotNull
        private BigDecimal threshold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStatementEmailRequest {
        @NotBlank
        private String userId;
        
        @NotBlank
        @Email
        private String email;
        
        @NotBlank
        private String firstName;
        
        @NotBlank
        private String accountNumber;
        
        @NotBlank
        private String month;
        
        @NotNull
        private BigDecimal openingBalance;
        
        @NotNull
        private BigDecimal closingBalance;
        
        private int transactionCount;
        
        @NotNull
        private BigDecimal totalCredits;
        
        @NotNull
        private BigDecimal totalDebits;
    }
}
