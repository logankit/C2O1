package com.equifax.c2o.api.ruleEngine.model.movecontract.types;

import lombok.Data;

@Data
public class MoveContractRequest {
    private String sourceContractId;
    private String targetContractId;
}
