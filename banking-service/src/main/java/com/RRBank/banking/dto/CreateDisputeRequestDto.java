package com.RRBank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDisputeRequestDto {
    
    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;
    
    @NotBlank(message = "Dispute type is required")
    private String disputeType;
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    private String description;
}
