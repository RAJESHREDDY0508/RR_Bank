package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Payee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayeeResponseDto {
    private String id;
    private String customerId;
    private String nickname;
    private String payeeName;
    private String payeeAccountNumber;
    private String payeeBankCode;
    private String payeeBankName;
    private String payeeRoutingNumber;
    private String payeeType;
    private String status;
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private BigDecimal transferLimit;
    private BigDecimal dailyLimit;
    private String email;
    private String phone;
    private Boolean isInternal;
    private LocalDateTime createdAt;

    public static PayeeResponseDto fromEntity(Payee payee) {
        return PayeeResponseDto.builder()
                .id(payee.getId().toString())
                .customerId(payee.getCustomerId().toString())
                .nickname(payee.getNickname())
                .payeeName(payee.getPayeeName())
                .payeeAccountNumber(payee.getPayeeAccountNumber())
                .payeeBankCode(payee.getPayeeBankCode())
                .payeeBankName(payee.getPayeeBankName())
                .payeeRoutingNumber(payee.getPayeeRoutingNumber())
                .payeeType(payee.getPayeeType().name())
                .status(payee.getStatus().name())
                .isVerified(payee.getIsVerified())
                .verifiedAt(payee.getVerifiedAt())
                .transferLimit(payee.getTransferLimit())
                .dailyLimit(payee.getDailyLimit())
                .email(payee.getEmail())
                .phone(payee.getPhone())
                .isInternal(payee.getIsInternal())
                .createdAt(payee.getCreatedAt())
                .build();
    }
}
