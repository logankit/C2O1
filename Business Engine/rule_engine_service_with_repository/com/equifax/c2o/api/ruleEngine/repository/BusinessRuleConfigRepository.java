
package com.equifax.c2o.api.ruleEngine.repository;

import com.equifax.c2o.api.ruleEngine.model.BusinessRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessRuleConfigRepository extends JpaRepository<BusinessRuleConfig, Long> {
    // Custom query methods can be added here if needed
}
