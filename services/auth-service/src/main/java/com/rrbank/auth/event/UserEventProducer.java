package com.rrbank.auth.event;

import com.rrbank.auth.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_EVENTS_TOPIC = "user-events";

    @Autowired(required = false)
    public UserEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserCreated(User user) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping USER_CREATED event for user: {}", user.getId());
            return;
        }
        
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

            kafkaTemplate.send(USER_EVENTS_TOPIC, user.getId().toString(), event);
            log.info("Published USER_CREATED event for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish USER_CREATED event: {}", e.getMessage());
        }
    }

    public void publishUserUpdated(User user) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping USER_UPDATED event for user: {}", user.getId());
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_UPDATED");
            event.put("userId", user.getId().toString());
            event.put("email", user.getEmail());
            event.put("status", user.getStatus().name());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(USER_EVENTS_TOPIC, user.getId().toString(), event);
            log.info("Published USER_UPDATED event for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish USER_UPDATED event: {}", e.getMessage());
        }
    }

    public void publishUserLogin(User user, String ipAddress, String userAgent) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping USER_LOGIN event for user: {}", user.getId());
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_LOGIN");
            event.put("userId", user.getId().toString());
            event.put("email", user.getEmail());
            event.put("ipAddress", ipAddress);
            event.put("userAgent", userAgent);
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(USER_EVENTS_TOPIC, user.getId().toString(), event);
            log.info("Published USER_LOGIN event for user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish USER_LOGIN event: {}", e.getMessage());
        }
    }

    public void publishUserLogout(String userId) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping USER_LOGOUT event for user: {}", userId);
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "USER_LOGOUT");
            event.put("userId", userId);
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(USER_EVENTS_TOPIC, userId, event);
            log.info("Published USER_LOGOUT event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish USER_LOGOUT event: {}", e.getMessage());
        }
    }

    public void publishFailedLogin(String usernameOrEmail, String ipAddress, String reason) {
        if (kafkaTemplate == null) {
            log.debug("Kafka disabled - skipping FAILED_LOGIN event");
            return;
        }
        
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "FAILED_LOGIN");
            event.put("usernameOrEmail", usernameOrEmail);
            event.put("ipAddress", ipAddress);
            event.put("reason", reason);
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(USER_EVENTS_TOPIC, usernameOrEmail, event);
            log.info("Published FAILED_LOGIN event for: {}", usernameOrEmail);
        } catch (Exception e) {
            log.error("Failed to publish FAILED_LOGIN event: {}", e.getMessage());
        }
    }
}
