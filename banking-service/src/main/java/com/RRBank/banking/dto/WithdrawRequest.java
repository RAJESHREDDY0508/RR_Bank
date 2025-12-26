package com.RRBank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Withdraw Request DTO
 * Used for POST /api/accounts/{accountId}/withdraw
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "50000.00", message = "Amount exceeds maximum withdrawal limit")
    private BigDecimal amount;
    
    private String currency;
    
    private String description;
}
