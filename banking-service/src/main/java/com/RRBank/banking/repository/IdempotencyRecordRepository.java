package com.RRBank.banking.repository;

import com.RRBank.banking.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);

    @Query("SELECT r FROM IdempotencyRecord r WHERE r.idempotencyKey = :key AND r.status = 'COMPLETED'")
    Optional<IdempotencyRecord> findCompletedByKey(@Param("key") String key);
}
