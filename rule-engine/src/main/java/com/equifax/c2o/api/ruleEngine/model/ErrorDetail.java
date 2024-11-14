package com.equifax.c2o.api.ruleEngine.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorDetail {
    private String code;
    private String message;
}
