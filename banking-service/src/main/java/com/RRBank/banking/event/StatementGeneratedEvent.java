package com.RRBank.banking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Statement Generated Event
 * Published when a statement is successfully generated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatementGeneratedEvent {

    private UUID statementId;
    private UUID accountId;
    private UUID customerId;
    private String statementPeriod;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private LocalDate startDate; // Alias for periodStartDate (for audit compatibility)
    private LocalDate endDate; // Alias for periodEndDate (for audit compatibility)
    private String statementType;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private Integer totalTransactions;
    private String pdfFilePath;
    private String csvFilePath;
    private String s3Bucket;
    private LocalDateTime generatedAt;
    
    @Builder.Default
    private String eventType = "STATEMENT_GENERATED";
}
