package com.RRBank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Deposit Request DTO
 * Used for POST /api/accounts/{accountId}/deposit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepositRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Amount exceeds maximum limit")
    private BigDecimal amount;
    
    private String currency;
    
    private String description;
}
