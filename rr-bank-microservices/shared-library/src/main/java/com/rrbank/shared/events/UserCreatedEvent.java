package com.rrbank.shared.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private String eventId;
    private LocalDateTime eventTime;
}
