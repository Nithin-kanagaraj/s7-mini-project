package com.hospital.scheduling.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComplianceRuleRepository extends JpaRepository<ComplianceRule, String> {
    Optional<ComplianceRule> findByRuleKey(String ruleKey);
}
