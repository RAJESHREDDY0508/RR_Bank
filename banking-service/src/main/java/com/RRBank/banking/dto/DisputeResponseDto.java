package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Dispute;
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
public class DisputeResponseDto {
    private String id;
    private String disputeNumber;
    private String transactionId;
    private String accountId;
    private String customerId;
    private BigDecimal disputedAmount;
    private String currency;
    private String disputeType;
    private String status;
    private String reason;
    private String customerDescription;
    private String merchantName;
    private LocalDateTime transactionDate;
    private Boolean provisionalCreditIssued;
    private BigDecimal provisionalCreditAmount;
    private String resolution;
    private String resolutionNotes;
    private BigDecimal refundAmount;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public static DisputeResponseDto fromEntity(Dispute dispute) {
        return DisputeResponseDto.builder()
                .id(dispute.getId().toString())
                .disputeNumber(dispute.getDisputeNumber())
                .transactionId(dispute.getTransactionId().toString())
                .accountId(dispute.getAccountId().toString())
                .customerId(dispute.getCustomerId().toString())
                .disputedAmount(dispute.getDisputedAmount())
                .currency(dispute.getCurrency())
                .disputeType(dispute.getDisputeType().name())
                .status(dispute.getStatus().name())
                .reason(dispute.getReason())
                .customerDescription(dispute.getCustomerDescription())
                .merchantName(dispute.getMerchantName())
                .transactionDate(dispute.getTransactionDate())
                .provisionalCreditIssued(dispute.getProvisionalCreditIssued())
                .provisionalCreditAmount(dispute.getProvisionalCreditAmount())
                .resolution(dispute.getResolution() != null ? dispute.getResolution().name() : null)
                .resolutionNotes(dispute.getResolutionNotes())
                .refundAmount(dispute.getRefundAmount())
                .dueDate(dispute.getDueDate())
                .createdAt(dispute.getCreatedAt())
                .resolvedAt(dispute.getResolvedAt())
                .build();
    }
}
