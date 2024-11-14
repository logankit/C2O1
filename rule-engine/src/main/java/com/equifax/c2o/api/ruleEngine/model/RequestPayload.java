package com.equifax.c2o.api.ruleEngine.model;

import com.equifax.c2o.api.ruleEngine.constants.APIConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RequestPayload {
    @NotBlank(message = APIConstants.MISSING_RULE_CODE)
    private String ruleCode;

    @NotNull(message = "inputData cannot be null.")
    private Object inputData;
}
