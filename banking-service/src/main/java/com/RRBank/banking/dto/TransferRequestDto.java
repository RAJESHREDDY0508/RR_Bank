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
 * Supports:
 * - TRANSFER: Both fromAccountId and toAccountId required
 * - DEPOSIT: Only toAccountId required (fromAccountId = null)
 * - WITHDRAWAL: Only fromAccountId required (toAccountId = null)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDto {

    /**
     * Source account ID (nullable for deposits)
     */
    @JsonProperty("fromAccountId")
    private UUID fromAccountId;

    /**
     * Destination account ID (nullable for withdrawals)
     */
    @JsonProperty("toAccountId")
    private UUID toAccountId;

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
