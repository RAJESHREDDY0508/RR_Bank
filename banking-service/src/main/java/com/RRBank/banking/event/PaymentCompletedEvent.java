package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Completed Event
 * Published when a payment completes successfully
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    private UUID paymentId;
    private String paymentReference;
    private UUID accountId;
    private UUID customerId;
    private String paymentType;
    private String payeeName;
    private BigDecimal amount;
    private String currency;
    private String gatewayTransactionId;
    private String status; // For audit compatibility
    private LocalDateTime completedAt;
    
    @Builder.Default
    private String eventType = "PAYMENT_COMPLETED";
}
