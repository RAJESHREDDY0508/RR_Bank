package com.rrbank.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent implements Serializable {
    private UUID transactionId;
    private String transactionReference;
    private UUID fromAccountId;
    private UUID toAccountId;
    private String transactionType;
    private BigDecimal amount;
    private String status;
    private String eventType;
    private LocalDateTime eventTime;
    private String eventId;
}
