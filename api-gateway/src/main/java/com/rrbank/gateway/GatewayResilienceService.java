package com.rrbank.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayResilienceService {

    /**
     * Example method protected by resilience logic
     * Never throw checked Exception from Gateway services
     */
    public String executeWithFallback(String operationName) {

        try {
            // Simulate protected operation
            return performOperation(operationName);
        } catch (Exception ex) {
            log.error("Gateway resilience fallback triggered for operation: {}", operationName, ex);
            return "SERVICE_UNAVAILABLE";
        }
    }

    /**
     * Internal operation (can throw Exception)
     */
    private String performOperation(String operationName) throws Exception {

        if ("FAIL".equalsIgnoreCase(operationName)) {
            throw new Exception("Simulated failure");
        }

        return "SUCCESS";
    }
}
