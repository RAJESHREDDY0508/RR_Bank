package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Response DTO
 * Used for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponseDto {

    private UUID id;
    private String transactionReference;
    private UUID fromAccountId;
    private String fromAccountNumber;
    private UUID toAccountId;
    private String toAccountNumber;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private String failureReason;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private LocalDateTime transactionDate;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /**
     * Convert Transaction entity to TransactionResponseDto
     */
    public static TransactionResponseDto fromEntity(Transaction transaction) {
        return TransactionResponseDto.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .fromAccountId(transaction.getFromAccountId())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountId(transaction.getToAccountId())
                .toAccountNumber(transaction.getToAccountNumber())
                .transactionType(transaction.getTransactionType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .description(transaction.getDescription())
                .failureReason(transaction.getFailureReason())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .transactionDate(transaction.getTransactionDate())
                .completedAt(transaction.getCompletedAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
