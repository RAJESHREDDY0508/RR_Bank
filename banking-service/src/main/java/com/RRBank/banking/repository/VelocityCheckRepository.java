package com.RRBank.banking.repository;

import com.RRBank.banking.entity.VelocityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VelocityCheckRepository extends JpaRepository<VelocityCheck, UUID> {

    List<VelocityCheck> findByUserId(String userId);

    Optional<VelocityCheck> findByUserIdAndCheckType(String userId, VelocityCheck.CheckType checkType);

    List<VelocityCheck> findByAccountId(UUID accountId);

    Optional<VelocityCheck> findByAccountIdAndCheckType(UUID accountId, VelocityCheck.CheckType checkType);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM VelocityCheck v " +
           "WHERE v.userId = :userId AND v.blockedUntil > CURRENT_TIMESTAMP")
    boolean isUserBlocked(@Param("userId") String userId);

    @Query("SELECT v FROM VelocityCheck v WHERE v.userId = :userId AND v.blockedUntil > CURRENT_TIMESTAMP")
    List<VelocityCheck> findBlockedByUserId(@Param("userId") String userId);
}
