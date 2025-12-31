package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Transfer Request DTO
 * Used for internal money transfers between accounts
 * 
 * Supports transfer by:
 * - Account ID (UUID): fromAccountId, toAccountId
 * - Account Number (String): fromAccountNumber, toAccountNumber
 * 
 * If both ID and Number are provided, ID takes precedence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDto {

    /**
     * Source account ID (UUID) - optional if fromAccountNumber is provided
     */
    @JsonProperty("fromAccountId")
    private UUID fromAccountId;

    /**
     * Destination account ID (UUID) - optional if toAccountNumber is provided
     */
    @JsonProperty("toAccountId")
    private UUID toAccountId;
    
    /**
     * Source account number (String) - optional if fromAccountId is provided
     */
    @JsonProperty("fromAccountNumber")
    private String fromAccountNumber;
    
    /**
     * Destination account number (String) - optional if toAccountId is provided
     */
    @JsonProperty("toAccountNumber")
    private String toAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    private String idempotencyKey; // For preventing duplicate transactions
    
    /**
     * Transaction type hint (optional - auto-detected if not provided)
     * Values: TRANSFER, DEPOSIT, WITHDRAWAL
     */
    private String transactionType;
}
