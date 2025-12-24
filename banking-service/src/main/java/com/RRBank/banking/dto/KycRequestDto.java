package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * KYC Request DTO
 * Used for KYC verification submission
 * 
 * Example JSON:
 * {
 *   "customerId": "550e8400-e29b-41d4-a716-446655440000",
 *   "documentType": "PASSPORT",
 *   "documentNumber": "AB1234567"
 * }
 * 
 * Valid document types: PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, SSN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRequestDto {

    @NotNull(message = "Customer ID is required")
    @JsonProperty("customerId")
    private UUID customerId;

    @NotBlank(message = "Document type is required")
    @Pattern(regexp = "^(PASSPORT|DRIVERS_LICENSE|NATIONAL_ID|SSN)$",
             message = "Document type must be one of: PASSPORT, DRIVERS_LICENSE, NATIONAL_ID, SSN")
    @JsonProperty("documentType")
    private String documentType;

    @NotBlank(message = "Document number is required")
    @JsonProperty("documentNumber")
    private String documentNumber;

    @JsonProperty("documentFrontUrl")
    private String documentFrontUrl; // URL to uploaded document image
    
    @JsonProperty("documentBackUrl")
    private String documentBackUrl;  // URL to back of document (if applicable)
    
    @JsonProperty("selfieUrl")
    private String selfieUrl;        // URL to selfie for verification
}
