package com.RRBank.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transaction Search Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSearchDto {

    private UUID accountId;
    private List<String> statuses; // PENDING, COMPLETED, FAILED, etc.
    private List<String> types; // TRANSFER, DEPOSIT, WITHDRAWAL, etc.
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description;
    private Integer page;
    private Integer size;
}
