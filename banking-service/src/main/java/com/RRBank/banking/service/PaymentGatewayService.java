package com.RRBank.banking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

/**
 * Payment Gateway Service
 * Mock implementation of external payment gateway integration
 * In production, this would integrate with real gateways like Stripe, PayPal, etc.
 */
@Service
@Slf4j
public class PaymentGatewayService {

    private final Random random = new Random();

    /**
     * Process bill payment through gateway
     * Mock implementation - in production would call actual gateway API
     */
    public GatewayResponse processBillPayment(
            String payeeName,
            String payeeAccount,
            BigDecimal amount,
            String paymentMethod) {
        
        log.info("Processing bill payment to {} for amount {} via {}", 
                payeeName, amount, paymentMethod);
        
        // Simulate gateway processing delay
        simulateProcessingDelay();
        
        // Simulate success/failure (95% success rate)
        boolean success = random.nextInt(100) < 95;
        
        if (success) {
            String gatewayTxnId = "GTW-" + UUID.randomUUID().toString();
            log.info("Bill payment processed successfully. Gateway TxnId: {}", gatewayTxnId);
            
            return GatewayResponse.builder()
                    .success(true)
                    .gatewayTransactionId(gatewayTxnId)
                    .responseMessage("Payment processed successfully")
                    .build();
        } else {
            log.warn("Bill payment failed at gateway");
            
            return GatewayResponse.builder()
                    .success(false)
                    .responseMessage("Gateway declined the payment")
                    .build();
        }
    }

    /**
     * Process merchant payment through gateway
     * Mock implementation
     */
    public GatewayResponse processMerchantPayment(
            String merchantName,
            String merchantId,
            BigDecimal amount,
            String paymentMethod) {
        
        log.info("Processing merchant payment to {} (ID: {}) for amount {} via {}", 
                merchantName, merchantId, amount, paymentMethod);
        
        // Simulate gateway processing delay
        simulateProcessingDelay();
        
        // Simulate success/failure (95% success rate)
        boolean success = random.nextInt(100) < 95;
        
        if (success) {
            String gatewayTxnId = "GTW-" + UUID.randomUUID().toString();
            log.info("Merchant payment processed successfully. Gateway TxnId: {}", gatewayTxnId);
            
            return GatewayResponse.builder()
                    .success(true)
                    .gatewayTransactionId(gatewayTxnId)
                    .responseMessage("Payment processed successfully")
                    .build();
        } else {
            log.warn("Merchant payment failed at gateway");
            
            return GatewayResponse.builder()
                    .success(false)
                    .responseMessage("Gateway declined the payment")
                    .build();
        }
    }

    /**
     * Verify payment status with gateway
     * Mock implementation
     */
    public GatewayResponse verifyPaymentStatus(String gatewayTransactionId) {
        log.info("Verifying payment status for gateway transaction: {}", gatewayTransactionId);
        
        // Mock verification - always returns success for valid ID
        if (gatewayTransactionId != null && gatewayTransactionId.startsWith("GTW-")) {
            return GatewayResponse.builder()
                    .success(true)
                    .gatewayTransactionId(gatewayTransactionId)
                    .responseMessage("Payment verified successfully")
                    .build();
        }
        
        return GatewayResponse.builder()
                .success(false)
                .responseMessage("Invalid gateway transaction ID")
                .build();
    }

    /**
     * Refund a payment through gateway
     * Mock implementation
     */
    public GatewayResponse refundPayment(String gatewayTransactionId, BigDecimal amount) {
        log.info("Processing refund for gateway transaction: {} amount: {}", 
                gatewayTransactionId, amount);
        
        simulateProcessingDelay();
        
        String refundTxnId = "REF-" + UUID.randomUUID().toString();
        
        return GatewayResponse.builder()
                .success(true)
                .gatewayTransactionId(refundTxnId)
                .responseMessage("Refund processed successfully")
                .build();
    }

    /**
     * Simulate gateway processing delay
     */
    private void simulateProcessingDelay() {
        try {
            // Simulate 100-500ms delay
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gateway Response DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GatewayResponse {
        private boolean success;
        private String gatewayTransactionId;
        private String responseMessage;
        private String errorCode;
    }
}
