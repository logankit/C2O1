package com.equifax.c2o.api.ruleEngine.repository;

import com.equifax.c2o.api.ruleEngine.entity.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {
    Optional<RuleConfig> findByRuleCode(String ruleCode);
}
