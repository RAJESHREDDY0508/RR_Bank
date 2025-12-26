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

    // Identifiers
    private UUID statementId;
    private UUID accountId;
    private UUID customerId;

    // Statement period (domain)
    private String statementPeriod;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;

    // Required by AuditEventsConsumer
    private LocalDate startDate;
    private LocalDate endDate;

    // Statement details
    private String statementType;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private Integer totalTransactions;

    // File locations
    private String pdfFilePath;
    private String csvFilePath;
    private String s3Bucket;

    // Timestamp
    private LocalDateTime generatedAt;

    // Event metadata
    @Builder.Default
    private String eventType = "STATEMENT_GENERATED";
}
