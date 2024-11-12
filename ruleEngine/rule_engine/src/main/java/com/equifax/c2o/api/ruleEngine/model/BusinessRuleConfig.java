
package com.equifax.c2o.api.ruleEngine.model;

import jakarta.persistence.*;

@Entity
@Table(name = "c2o_re_bus_rule_config")
public class BusinessRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @Lob
    @Column(name = "input_schema", nullable = true)
    private String inputSchema;

    // Getters and Setters

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }
}
