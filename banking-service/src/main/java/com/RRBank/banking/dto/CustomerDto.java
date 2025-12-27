package com.RRBank.banking.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String kycStatus;
    private String customerSegment;
    private LocalDateTime kycVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
