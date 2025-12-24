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

    private UUID paymentId;
    private String paymentReference;
    private UUID accountId;
    private UUID customerId;
    private String paymentType;
    private String payeeName;
    private BigDecimal amount;
    private String currency;
    private String failureReason;
    private String reason; // Alias for failureReason (for audit compatibility)
    private String status; // For audit compatibility
    private LocalDateTime failedAt;
    
    @Builder.Default
    private String eventType = "PAYMENT_FAILED";
}
