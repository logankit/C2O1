package com.equifax.c2o.api.ruleEngine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "c2o_re_bus_rule_config")
@Data
public class RuleConfig {

    @Id
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_code")
    private String ruleCode;

    @Lob
    @Column(name = "input_schema")
    private String inputSchema;
}
