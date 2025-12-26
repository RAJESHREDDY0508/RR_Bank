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

    private String transactionId;     // API-friendly ID
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
        if (transaction == null) return null;

        return TransactionResponse.builder()
                // ✅ FIX: canonical UUID → String
                .transactionId(transaction.getId() != null
                        ? transaction.getId().toString()
                        : null)

                // ✅ FIX: correct field name
                .referenceNumber(transaction.getTransactionReference())

                .transactionType(transaction.getTransactionType() != null
                        ? transaction.getTransactionType().name()
                        : null)

                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())

                .status(transaction.getStatus() != null
                        ? transaction.getStatus().name()
                        : null)

                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .fee(transaction.getFee())

                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountNumber(transaction.getToAccountNumber())
                .merchantName(transaction.getMerchantName())

                .transactionDate(transaction.getTransactionDate())
                .completedDate(transaction.getCompletedAt())
                .build();
    }
}
