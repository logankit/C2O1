package com.equifax.c2o.api.ruleEngine.model.moveaccount.types;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveAccountRequest {
    @JsonProperty("sourceContractId")
    @JsonAlias("sourceContract")
    private BigDecimal sourceContractId;
    
    @JsonProperty("targetContractId")
    @JsonAlias("targetContract")
    private BigDecimal targetContractId;
    
    private List<ShipTo> shipTos;
    private List<String> billTos;
}
