package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Created Event
 * Published to Kafka when a new customer is created
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCreatedEvent {

    private UUID customerId;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDateTime createdAt;
    
    @Builder.Default
    private String eventType = "CUSTOMER_CREATED";
}
