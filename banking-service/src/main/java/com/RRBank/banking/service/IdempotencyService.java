package com.RRBank.banking.service;

import com.RRBank.banking.entity.IdempotencyRecord;
import com.RRBank.banking.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotency Service - Prevent duplicate transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private static final int DEFAULT_EXPIRY_HOURS = 24;

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> checkDuplicate(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return idempotencyRecordRepository.findCompletedByKey(idempotencyKey);
    }

    @Transactional
    public IdempotencyRecord checkOrCreate(String idempotencyKey, String requestHash) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        
        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
            .findByIdempotencyKey(idempotencyKey);
        
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IllegalStateException("Idempotency key already used with different request");
            }
            return record;
        }
        
        IdempotencyRecord newRecord = IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .requestHash(requestHash)
            .status(IdempotencyRecord.Status.PENDING)
            .expiresAt(LocalDateTime.now().plusHours(DEFAULT_EXPIRY_HOURS))
            .build();
        
        return idempotencyRecordRepository.save(newRecord);
    }

    @Transactional
    public void markCompleted(String idempotencyKey, UUID transactionId, Map<String, Object> responseData) {
        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
            .ifPresent(record -> {
                record.setStatus(IdempotencyRecord.Status.COMPLETED);
                record.setTransactionId(transactionId);
                record.setResponseData(responseData);
                idempotencyRecordRepository.save(record);
            });
    }

    @Transactional
    public void markFailed(String idempotencyKey) {
        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
            .ifPresent(record -> {
                record.setStatus(IdempotencyRecord.Status.FAILED);
                idempotencyRecordRepository.save(record);
            });
    }

    public String generateRequestHash(Object... params) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Object param : params) {
                sb.append(param != null ? param.toString() : "null").append("|");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Store a new idempotency record after successful operation
     */
    @Transactional
    public void storeRecord(String idempotencyKey, String resourceId, String resourceType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        
        IdempotencyRecord record = IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .requestHash(resourceType + ":" + resourceId)
            .status(IdempotencyRecord.Status.COMPLETED)
            .transactionId(UUID.fromString(resourceId))
            .expiresAt(LocalDateTime.now().plusHours(DEFAULT_EXPIRY_HOURS))
            .build();
        
        idempotencyRecordRepository.save(record);
        log.debug("Stored idempotency record for key: {}", idempotencyKey);
    }

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupExpiredRecords() {
        int deleted = idempotencyRecordRepository.deleteExpiredRecords(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency records", deleted);
        }
    }
}
