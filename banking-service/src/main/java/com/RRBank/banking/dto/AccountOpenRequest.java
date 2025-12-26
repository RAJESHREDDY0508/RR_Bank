package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Account;
import com.RRBank.banking.entity.AccountRequest;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOpenRequest {
    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;
    
    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit;
    
    private String currency;
    private String notes;
}
