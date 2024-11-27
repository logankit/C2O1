package com.equifax.c2o.api.ruleEngine.model.moveaccount.types;

import java.util.List;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShipTo {
    // obaNumbers is an array of strings
    private List<String> obaNumbers;
    private String targetBillTo;
}