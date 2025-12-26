package com.RRBank.banking.dto;

import com.RRBank.banking.entity.AccountRequest;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountRequestResponse {
    private UUID id;
    private String userId;
    private String accountType;
    private BigDecimal initialDeposit;
    private String currency;
    private String status;
    private String requestNotes;
    private String adminNotes;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private UUID accountId;
    private LocalDateTime createdAt;
    
    public static AccountRequestResponse fromEntity(AccountRequest request) {
        return AccountRequestResponse.builder()
            .id(request.getId())
            .userId(request.getUserId())
            .accountType(request.getAccountType().name())
            .initialDeposit(request.getInitialDeposit())
            .currency(request.getCurrency())
            .status(request.getStatus().name())
            .requestNotes(request.getRequestNotes())
            .adminNotes(request.getAdminNotes())
            .reviewedBy(request.getReviewedBy())
            .reviewedAt(request.getReviewedAt())
            .accountId(request.getAccountId())
            .createdAt(request.getCreatedAt())
            .build();
    }
}
