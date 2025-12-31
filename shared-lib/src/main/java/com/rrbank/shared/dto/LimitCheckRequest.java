package com.rrbank.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to check transaction limits and fraud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitCheckRequest {
    private String userId;
    private String accountId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String ipAddress;
    private String deviceFingerprint;
    private String locationCountry;
    private String locationCity;
}
