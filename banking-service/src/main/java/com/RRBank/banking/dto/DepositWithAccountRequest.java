package com.RRBank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deposit With Account Request DTO
 * ✅ FIX: Used for POST /api/transactions/deposit (alternative endpoint)
 * Accepts either accountId (UUID) or accountNumber (String)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositWithAccountRequest {
    
    // Either accountId or accountNumber must be provided
    private UUID accountId;
    
    private String accountNumber;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Amount exceeds maximum limit")
    private BigDecimal amount;
    
    private String currency;
    
    private String description;
}
