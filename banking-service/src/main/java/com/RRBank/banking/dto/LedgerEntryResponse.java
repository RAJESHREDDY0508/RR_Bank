package com.RRBank.banking.dto;

import com.RRBank.banking.entity.LedgerEntry;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryResponse {
    private UUID id;
    private UUID accountId;
    private UUID transactionId;
    private String entryType;
    private BigDecimal amount;
    private String currency;
    private BigDecimal runningBalance;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
    
    public static LedgerEntryResponse fromEntity(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
            .id(entry.getId())
            .accountId(entry.getAccountId())
            .transactionId(entry.getTransactionId())
            .entryType(entry.getEntryType().name())
            .amount(entry.getAmount())
            .currency(entry.getCurrency())
            .runningBalance(entry.getRunningBalance())
            .referenceId(entry.getReferenceId())
            .description(entry.getDescription())
            .createdAt(entry.getCreatedAt())
            .build();
    }
}
