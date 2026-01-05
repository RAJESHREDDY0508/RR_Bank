package com.rrbank.fraud.controller;

import com.rrbank.fraud.dto.FraudDTOs.*;
import com.rrbank.fraud.service.FraudCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudController {

    private final FraudCheckService fraudCheckService;

    @PostMapping("/check")
    public ResponseEntity<FraudCheckResponse> checkTransaction(@Valid @RequestBody FraudCheckRequest request) {
        log.info("Fraud check request for account: {}, amount: {}", request.getAccountId(), request.getAmount());
        FraudCheckResponse response = fraudCheckService.checkTransaction(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/limits/{userId}")
    public ResponseEntity<UserLimitsResponse> getUserLimits(@PathVariable String userId) {
        log.info("Get limits for user: {}", userId);
        return ResponseEntity.ok(fraudCheckService.getUserLimits(userId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Service is healthy");
    }
}
