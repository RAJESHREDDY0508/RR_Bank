package com.RRBank.banking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Account Status DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountStatusDto {

    @NotNull(message = "Status is required")
    private String status; // ACTIVE, FROZEN, CLOSED, SUSPENDED

    private String reason; // Reason for status change
}
