package com.RRBank.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Merchant Payment Request DTO
 * Used for creating merchant payments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantPaymentRequestDto {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotBlank(message = "Merchant name is required")
    private String merchantName;

    @NotBlank(message = "Merchant ID is required")
    private String merchantId;

    private String orderReference; // Order ID, invoice number, etc.

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // DEBIT_CARD, CREDIT_CARD, WALLET
}
