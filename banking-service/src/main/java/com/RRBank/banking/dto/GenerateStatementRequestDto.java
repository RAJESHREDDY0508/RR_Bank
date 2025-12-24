package com.RRBank.banking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Generate Statement Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateStatementRequestDto {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private String statementType; // MONTHLY, QUARTERLY, ANNUAL, ON_DEMAND

    private Boolean includePdf;

    private Boolean includeCsv;
}
