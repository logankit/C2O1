package com.equifax.c2o.api.ruleEngine.model.moveaccountgroup.types;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class MoveAccountGroupRequest {
    @JsonProperty("sourceContract")
    @JsonAlias("sourceContractId")
    private BigDecimal sourceContractId;
    
    @JsonProperty("targetContract")
    @JsonAlias("targetContractId")
    private BigDecimal targetContractId;
    
    private ShipToGroup shipToGroup;
    
    @Data
    public static class ShipToGroup {
        private List<String> obaNumbers;
        private String targetBillTo;
    }
}
