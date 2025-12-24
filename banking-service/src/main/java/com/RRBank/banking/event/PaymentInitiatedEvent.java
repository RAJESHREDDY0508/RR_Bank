package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Initiated Event
 * Published when a payment is initiated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiatedEvent {

    private UUID paymentId;
    private String paymentReference;
    private UUID accountId;
    private UUID customerId;
    private String paymentType;
    private String payeeName;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime initiatedAt;
    
    @Builder.Default
    private String eventType = "PAYMENT_INITIATED";
}
