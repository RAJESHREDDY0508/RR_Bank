package com.RRBank.banking.dto;

import com.RRBank.banking.entity.Statement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Statement Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementResponseDto {

    private UUID id;
    private UUID accountId;
    private UUID customerId;
    private String statementPeriod;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private String statementType;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private Integer totalTransactions;
    private String status;
    private String pdfFilePath;
    private String csvFilePath;
    private Long pdfFileSize;
    private Long csvFileSize;
    private LocalDateTime generatedAt;
    private Integer downloadCount;
    private LocalDateTime createdAt;

    public static StatementResponseDto fromEntity(Statement statement) {
        return StatementResponseDto.builder()
                .id(statement.getId())
                .accountId(statement.getAccountId())
                .customerId(statement.getCustomerId())
                .statementPeriod(statement.getStatementPeriod())
                .periodStartDate(statement.getPeriodStartDate())
                .periodEndDate(statement.getPeriodEndDate())
                .statementType(statement.getStatementType().name())
                .openingBalance(statement.getOpeningBalance())
                .closingBalance(statement.getClosingBalance())
                .totalDeposits(statement.getTotalDeposits())
                .totalWithdrawals(statement.getTotalWithdrawals())
                .totalTransactions(statement.getTotalTransactions())
                .status(statement.getStatus().name())
                .pdfFilePath(statement.getPdfFilePath())
                .csvFilePath(statement.getCsvFilePath())
                .pdfFileSize(statement.getPdfFileSize())
                .csvFileSize(statement.getCsvFileSize())
                .generatedAt(statement.getGeneratedAt())
                .downloadCount(statement.getDownloadCount())
                .createdAt(statement.getCreatedAt())
                .build();
    }
}
