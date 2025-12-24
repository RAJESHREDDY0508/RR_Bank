package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Transaction;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transaction responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    private String transactionId;
    private String referenceNumber;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal fee;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String merchantName;
    private LocalDateTime transactionDate;
    private LocalDateTime completedDate;
    
    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
            .transactionId(transaction.getTransactionId())
            .referenceNumber(transaction.getReferenceNumber())
            .transactionType(transaction.getTransactionType().name())
            .amount(transaction.getAmount())
            .currency(transaction.getCurrency())
            .description(transaction.getDescription())
            .status(transaction.getStatus().name())
            .balanceBefore(transaction.getBalanceBefore())
            .balanceAfter(transaction.getBalanceAfter())
            .fee(transaction.getFee())
            .fromAccountNumber(transaction.getFromAccountNumber())
            .toAccountNumber(transaction.getToAccountNumber())
            .merchantName(transaction.getMerchantName())
            .transactionDate(transaction.getTransactionDate())
            .completedDate(transaction.getCompletedDate())
            .build();
    }
}
