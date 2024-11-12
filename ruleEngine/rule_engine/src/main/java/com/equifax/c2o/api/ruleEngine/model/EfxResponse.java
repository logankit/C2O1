
package com.equifax.c2o.api.ruleEngine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class EfxResponse {
    private String status;
    private String message;
    private List<DataItem> data = new ArrayList<>();
}
