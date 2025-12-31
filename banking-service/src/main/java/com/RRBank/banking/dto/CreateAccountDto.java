package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Create Account Request DTO
 * Used when creating a new bank account
 * 
 * Example JSON (with customerId - for admin/backward compatibility):
 * {
 *   "customerId": "550e8400-e29b-41d4-a716-446655440000",
 *   "accountType": "SAVINGS",
 *   "initialBalance": 1000.00,
 *   "currency": "USD"
 * }
 * 
 * Example JSON (without customerId - auto-detected from authenticated user):
 * {
 *   "accountType": "SAVINGS",
 *   "initialBalance": 1000.00,
 *   "currency": "USD"
 * }
 * 
 * Valid account types: SAVINGS, CHECKING, CREDIT, BUSINESS
 * 
 * ✅ FIX: Made customerId optional - will be auto-detected from authenticated user if not provided
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountDto {

    // ✅ FIX: Made customerId optional - will be resolved from authenticated user if not provided
    @JsonProperty("customerId")
    private UUID customerId;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "^(SAVINGS|CHECKING|CREDIT|BUSINESS)$", 
             message = "Account type must be one of: SAVINGS, CHECKING, CREDIT, BUSINESS")
    @JsonProperty("accountType")
    private String accountType;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
    @JsonProperty("initialBalance")
    private BigDecimal initialBalance;

    @JsonProperty("currency")
    @Builder.Default
    private String currency = "USD";

    @DecimalMin(value = "0.0", inclusive = true, message = "Overdraft limit cannot be negative")
    @JsonProperty("overdraftLimit")
    private BigDecimal overdraftLimit;

    @DecimalMin(value = "0.0", inclusive = true, message = "Interest rate cannot be negative")
    @JsonProperty("interestRate")
    private BigDecimal interestRate;
}
