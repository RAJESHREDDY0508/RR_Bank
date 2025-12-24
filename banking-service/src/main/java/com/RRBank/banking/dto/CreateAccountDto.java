package com.RRBank.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Create Account Request DTO
 * Used when creating a new bank account
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountDto {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Account type is required")
    private String accountType; // SAVINGS, CHECKING, CREDIT, BUSINESS

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
    private BigDecimal initialBalance;

    private String currency; // Default: USD

    @DecimalMin(value = "0.0", inclusive = true, message = "Overdraft limit cannot be negative")
    private BigDecimal overdraftLimit;

    @DecimalMin(value = "0.0", inclusive = true, message = "Interest rate cannot be negative")
    private BigDecimal interestRate;
}
