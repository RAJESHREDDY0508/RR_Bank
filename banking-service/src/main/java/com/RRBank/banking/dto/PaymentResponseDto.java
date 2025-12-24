package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Response DTO
 * Used for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    private UUID id;
    private String paymentReference;
    private UUID accountId;
    private String accountNumber;
    private UUID customerId;
    private String paymentType;
    private String payeeName;
    private String payeeAccount;
    private String payeeReference;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String description;
    private LocalDate scheduledDate;
    private LocalDateTime processedAt;
    private String gatewayTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;

    /**
     * Convert Payment entity to PaymentResponseDto
     */
    public static PaymentResponseDto fromEntity(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .paymentReference(payment.getPaymentReference())
                .accountId(payment.getAccountId())
                .customerId(payment.getCustomerId())
                .paymentType(payment.getPaymentType().name())
                .payeeName(payment.getPayeeName())
                .payeeAccount(payment.getPayeeAccount())
                .payeeReference(payment.getPayeeReference())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null)
                .description(payment.getDescription())
                .scheduledDate(payment.getScheduledDate())
                .processedAt(payment.getProcessedAt())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
