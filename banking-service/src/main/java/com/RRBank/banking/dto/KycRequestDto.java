package com.RRBank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * KYC Request DTO
 * Used for KYC verification submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRequestDto {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Document type is required")
    private String documentType; // PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, SSN

    @NotBlank(message = "Document number is required")
    private String documentNumber;

    private String documentFrontUrl; // URL to uploaded document image
    private String documentBackUrl;  // URL to back of document (if applicable)
    private String selfieUrl;        // URL to selfie for verification
}
