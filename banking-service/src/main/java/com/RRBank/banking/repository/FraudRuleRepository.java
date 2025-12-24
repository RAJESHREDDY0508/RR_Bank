package com.RRBank.banking.repository;

import com.RRBank.banking.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Fraud Rule Repository
 */
@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {

    /**
     * Find all enabled rules ordered by priority
     */
    @Query("SELECT r FROM FraudRule r WHERE r.enabled = true ORDER BY r.priority DESC")
    List<FraudRule> findAllEnabledOrderByPriority();

    /**
     * Find by rule type (String)
     */
    List<FraudRule> findByRuleType(String ruleType);

    /**
     * Find enabled rules by type
     */
    List<FraudRule> findByRuleTypeAndEnabledTrue(String ruleType);

    /**
     * Count enabled rules
     */
    long countByEnabledTrue();
}
