package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Failed Event
 * Published when a payment fails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {

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

    // Failure details
    private String failureReason;

    // Required by AuditEventsConsumer
    private String reason;
    private String status;

    // Timestamp
    private LocalDateTime failedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "PAYMENT_FAILED";
}
