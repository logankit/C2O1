package com.equifax.c2o.api.ruleEngine.model.moveaccount.types;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveAccountRequest {
    @JsonProperty(value = "sourceContractId", alternative = "sourceContract")
    private BigDecimal sourceContractId;
    
    @JsonProperty(value = "targetContractId", alternative = "targetContract")
    private BigDecimal targetContractId;
    
    private List<ShipTo> shipTos;
    private List<String> billTos;
}
