package com.equifax.c2o.api.ruleEngine.model;

import lombok.Data;

import java.util.List;

@Data
public class ApiResponse {
    private String status;
    private String code;
    private String message;
    private List<ErrorDetail> data;
}
