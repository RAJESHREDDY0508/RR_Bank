package com.RRBank.banking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Account Status DTO
 * 
 * Example JSON:
 * {
 *   "status": "ACTIVE",
 *   "reason": "Account verified and approved"
 * }
 * 
 * Valid statuses: ACTIVE, FROZEN, CLOSED, SUSPENDED, PENDING
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAccountStatusDto {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|FROZEN|CLOSED|SUSPENDED|PENDING)$", 
             message = "Status must be one of: ACTIVE, FROZEN, CLOSED, SUSPENDED, PENDING")
    @JsonProperty("status")
    private String status;

    @JsonProperty("reason")
    private String reason; // Reason for status change
}
