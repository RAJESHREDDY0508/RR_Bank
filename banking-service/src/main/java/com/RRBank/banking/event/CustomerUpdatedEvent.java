package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Customer Updated Event
 * Published to Kafka when customer profile is updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUpdatedEvent {

    private UUID customerId;
    private UUID userId;
    private Map<String, Object> updatedFields; // Track which fields were updated
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private String eventType = "CUSTOMER_UPDATED";
}
