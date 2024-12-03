package com.equifax.c2o.api.ruleEngine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetail {
    private String code;
    private String message;
    private String entity;

    public ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
