package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * KYC Verified Event
 * Published to Kafka when customer KYC is verified
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycVerifiedEvent {

    private UUID customerId;
    private UUID userId;
    private String kycStatus; // VERIFIED, REJECTED
    private String documentType;
    private LocalDateTime verifiedAt;
    private String verifiedBy; // Admin user who verified
    
    @Builder.Default
    private String eventType = "KYC_VERIFIED";
}
