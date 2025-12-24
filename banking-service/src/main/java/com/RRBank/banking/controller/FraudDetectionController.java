package com.RRBank.banking.controller;

import com.RRBank.banking.dto.*;
import com.RRBank.banking.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Fraud Detection Controller
 * REST API endpoints for fraud management
 */
@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    /**
     * Get all fraud alerts
     * GET /api/fraud/alerts
     */
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudEventResponseDto>> getAllAlerts() {
        log.info("REST request to get all fraud alerts");
        List<FraudEventResponseDto> alerts = fraudDetectionService.getAllAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get high-risk fraud alerts
     * GET /api/fraud/alerts/high-risk
     */
    @GetMapping("/alerts/high-risk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudEventResponseDto>> getHighRiskAlerts() {
        log.info("REST request to get high-risk fraud alerts");
        List<FraudEventResponseDto> alerts = fraudDetectionService.getHighRiskAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get recent fraud alerts (last 24 hours)
     * GET /api/fraud/alerts/recent
     */
    @GetMapping("/alerts/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudEventResponseDto>> getRecentAlerts() {
        log.info("REST request to get recent fraud alerts");
        List<FraudEventResponseDto> alerts = fraudDetectionService.getRecentAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get fraud alerts by account
     * GET /api/fraud/alerts/account/{accountId}
     */
    @GetMapping("/alerts/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudEventResponseDto>> getAlertsByAccount(@PathVariable UUID accountId) {
        log.info("REST request to get fraud alerts for accountId: {}", accountId);
        List<FraudEventResponseDto> alerts = fraudDetectionService.getAlertsByAccount(accountId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get risk score for transaction
     * GET /api/fraud/score/{transactionId}
     */
    @GetMapping("/score/{transactionId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<RiskScoreResponseDto> getRiskScore(@PathVariable UUID transactionId) {
        log.info("REST request to get risk score for transactionId: {}", transactionId);
        RiskScoreResponseDto riskScore = fraudDetectionService.getRiskScore(transactionId);
        return ResponseEntity.ok(riskScore);
    }

    /**
     * Get all fraud rules
     * GET /api/fraud/rules
     */
    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudRuleResponseDto>> getAllRules() {
        log.info("REST request to get all fraud rules");
        List<FraudRuleResponseDto> rules = fraudDetectionService.getAllRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get enabled fraud rules
     * GET /api/fraud/rules/enabled
     */
    @GetMapping("/rules/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FraudRuleResponseDto>> getEnabledRules() {
        log.info("REST request to get enabled fraud rules");
        List<FraudRuleResponseDto> rules = fraudDetectionService.getEnabledRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Create fraud rule
     * POST /api/fraud/rules
     */
    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudRuleResponseDto> createRule(
            @Valid @RequestBody FraudRuleRequestDto request,
            Authentication authentication) {
        log.info("REST request to create fraud rule: {}", request.getRuleName());
        
        UUID createdBy = extractUserIdFromAuthentication(authentication);
        
        FraudRuleResponseDto rule = fraudDetectionService.createRule(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    /**
     * Update fraud rule
     * PUT /api/fraud/rules/{id}
     */
    @PutMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FraudRuleResponseDto> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody FraudRuleRequestDto request) {
        log.info("REST request to update fraud rule: {}", id);
        
        FraudRuleResponseDto rule = fraudDetectionService.updateRule(id, request);
        return ResponseEntity.ok(rule);
    }

    /**
     * Delete fraud rule
     * DELETE /api/fraud/rules/{id}
     */
    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        log.info("REST request to delete fraud rule: {}", id);
        
        fraudDetectionService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    // ========== HELPER METHODS ==========

    private UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            try {
                return UUID.fromString(authentication.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse userId from authentication: {}", authentication.getName());
                return UUID.randomUUID(); // Placeholder
            }
        }
        return UUID.randomUUID(); // Placeholder
    }
}
