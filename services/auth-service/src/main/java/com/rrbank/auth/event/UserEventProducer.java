package com.rrbank.auth.event;

import com.rrbank.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_CREATED_TOPIC = "user-created";
    private static final String USER_UPDATED_TOPIC = "user-updated";

    public void publishUserCreated(User user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_CREATED");
            event.put("userId", user.getId().toString());
            event.put("email", user.getEmail());
            event.put("username", user.getUsername());
            event.put("firstName", user.getFirstName());
            event.put("lastName", user.getLastName());
            event.put("role", user.getRole().name());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(USER_CREATED_TOPIC, user.getId().toString(), event);
            log.info("Published USER_CREATED event for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish USER_CREATED event: {}", e.getMessage());
        }
    }

    public void publishUserUpdated(User user) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_UPDATED");
            event.put("userId", user.getId().toString());
            event.put("email", user.getEmail());
            event.put("status", user.getStatus().name());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(USER_UPDATED_TOPIC, user.getId().toString(), event);
            log.info("Published USER_UPDATED event for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish USER_UPDATED event: {}", e.getMessage());
        }
    }
}
