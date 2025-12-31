package com.RRBank.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePayeeRequestDto {
    
    @NotNull(message = "Customer ID is required")
    private UUID customerId;
    
    @NotBlank(message = "Nickname is required")
    private String nickname;
    
    @NotBlank(message = "Payee name is required")
    private String payeeName;
    
    @NotBlank(message = "Payee account number is required")
    private String payeeAccountNumber;
    
    private String payeeBankCode;
    private String payeeBankName;
    private String payeeRoutingNumber;
    private String payeeType;
    private BigDecimal transferLimit;
    private BigDecimal dailyLimit;
    private String email;
    private String phone;
    private String notes;
}
