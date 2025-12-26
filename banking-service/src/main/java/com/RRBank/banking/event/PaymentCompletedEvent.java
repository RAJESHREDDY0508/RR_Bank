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

    // Identifiers
    private UUID paymentId;
    private String paymentReference;

    // Primary account (used by AuditEventsConsumer)
    private UUID accountId;

    // Domain context
    private UUID customerId;
    private String paymentType;
    private String payeeName;

    // Payment details
    private BigDecimal amount;
    private String currency;
    private String gatewayTransactionId;

    // Required by AuditEventsConsumer
    private String status;

    // Timestamp
    private LocalDateTime completedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "PAYMENT_COMPLETED";
}
