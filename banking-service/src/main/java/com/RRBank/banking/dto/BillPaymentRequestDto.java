package com.RRBank.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Bill Payment Request DTO
 * Used for creating bill payments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPaymentRequestDto {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotBlank(message = "Payee name is required")
    private String payeeName;

    @NotBlank(message = "Payee account is required")
    private String payeeAccount;

    private String payeeReference; // Bill number, account number, etc.

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    private LocalDate scheduledDate; // For scheduled payments

    private String paymentMethod; // DEBIT_CARD, CREDIT_CARD, ACH, WIRE
}
