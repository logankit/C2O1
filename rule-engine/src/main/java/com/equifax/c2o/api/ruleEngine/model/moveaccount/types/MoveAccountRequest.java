package com.equifax.c2o.api.ruleEngine.model.moveaccount.types;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveAccountRequest {
    private BigDecimal sourceContractId;
    private BigDecimal targetContractId;
    private List<ShipTo> shipTos;
    private List<String> billTos;
}
