package com.equifax.c2o.api.ruleEngine.model;

import com.equifax.c2o.api.ruleEngine.constants.APIConstants;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestPayload {
    @NotBlank(message = APIConstants.MISSING_RULE_CODE)
    @Size(max = 50, message = "Rule code cannot exceed 50 characters")
    private String ruleCode;

    @NotNull(message = "inputData cannot be null")
    private JsonNode inputData;
}
