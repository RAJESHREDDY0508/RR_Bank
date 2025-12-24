package com.RRBank.banking.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO for transfer requests between accounts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    
    @NotBlank(message = "From account number is required")
    private String fromAccountNumber;
    
    @NotBlank(message = "To account number is required")
    private String toAccountNumber;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    @Builder.Default
    private String currency = "USD";
}
