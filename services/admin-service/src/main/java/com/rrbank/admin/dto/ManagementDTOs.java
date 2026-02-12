package com.rrbank.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ManagementDTOs {

    // ==================== Customer Management ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerResponse {
        private UUID id;
        private UUID userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String fullName;
        private String phoneNumber;
        private String status;
        private String kycStatus;
        private LocalDateTime dateOfBirth;
        private AddressInfo address;
        private int accountCount;
        private BigDecimal totalBalance;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressInfo {
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCustomerStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;

        private String reason;
    }

    // ==================== Account Management ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountResponse {
        private UUID id;
        private String accountNumber;
        private String accountType;
        private String status;
        private String currency;
        private BigDecimal balance;
        private BigDecimal availableBalance;
        private UUID userId;
        private UUID customerId;
        private String customerName;
        private String customerEmail;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime frozenAt;
        private String frozenReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountActionRequest {
        @NotBlank(message = "Action is required")
        private String action; // FREEZE, UNFREEZE, CLOSE

        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountRequestResponse {
        private UUID id;
        private UUID userId;
        private String customerName;
        private String customerEmail;
        private String accountType;
        private String status;
        private String notes;
        private BigDecimal initialDeposit;
        private LocalDateTime requestedAt;
        private LocalDateTime processedAt;
        private String processedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessAccountRequestDTO {
        @NotBlank(message = "Decision is required")
        private String decision; // APPROVE, REJECT

        private String notes;
    }

    // ==================== Transaction Management ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private UUID id;
        private String transactionReference;
        private String transactionType;
        private String status;
        private BigDecimal amount;
        private String currency;
        private UUID fromAccountId;
        private String fromAccountNumber;
        private UUID toAccountId;
        private String toAccountNumber;
        private String description;
        private String failureReason;
        private UUID initiatedBy;
        private String initiatedByName;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionFilterRequest {
        private String transactionType;
        private String status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private String accountNumber;
        private UUID customerId;
    }

    // ==================== Fraud & Limits ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudAlertResponse {
        private UUID id;
        private UUID accountId;
        private String accountNumber;
        private UUID userId;
        private String customerName;
        private UUID transactionId;
        private String transactionRef;
        private String eventType;
        private String decision;
        private String reason;
        private BigDecimal riskScore;
        private String status;
        private String resolvedBy;
        private LocalDateTime resolvedAt;
        private String resolutionNotes;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveFraudAlertRequest {
        @NotBlank(message = "Resolution is required")
        private String resolution; // CONFIRMED_FRAUD, FALSE_POSITIVE, NEEDS_REVIEW

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionLimitResponse {
        private UUID id;
        private UUID userId;
        private String customerName;
        private String limitType;
        private BigDecimal dailyLimit;
        private BigDecimal perTransactionLimit;
        private BigDecimal monthlyLimit;
        private BigDecimal usedToday;
        private BigDecimal usedThisMonth;
        private BigDecimal remainingDaily;
        private BigDecimal remainingMonthly;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLimitRequest {
        @NotNull(message = "Daily limit is required")
        private BigDecimal dailyLimit;

        @NotNull(message = "Per transaction limit is required")
        private BigDecimal perTransactionLimit;

        @NotNull(message = "Monthly limit is required")
        private BigDecimal monthlyLimit;

        private String reason;
    }

    // ==================== Audit Logs ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogResponse {
        private UUID id;
        private UUID adminUserId;
        private String adminUsername;
        private String action;
        private String actionType;
        private String entityType;
        private String entityId;
        private String description;
        private String oldValue;
        private String newValue;
        private String ipAddress;
        private String userAgent;
        private String status;
        private String errorMessage;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogFilterRequest {
        private UUID adminUserId;
        private String action;
        private String actionType;
        private String entityType;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    // ==================== Statements ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatementResponse {
        private UUID id;
        private UUID accountId;
        private String accountNumber;
        private String customerName;
        private String period;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String status;
        private String downloadUrl;
        private Long fileSize;
        private LocalDateTime generatedAt;
        private String generatedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateStatementRequest {
        @NotNull(message = "Account ID is required")
        private UUID accountId;

        @NotNull(message = "Start date is required")
        private LocalDateTime startDate;

        @NotNull(message = "End date is required")
        private LocalDateTime endDate;
    }
}
