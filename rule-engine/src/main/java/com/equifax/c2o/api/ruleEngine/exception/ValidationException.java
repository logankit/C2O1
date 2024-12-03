package com.equifax.c2o.api.ruleEngine.exception;

import java.util.List;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;

import lombok.Getter;

@Getter
public class ValidationException extends Exception {
    private final String code;
    private final List<ErrorDetail> data;

    public ValidationException(String code, String message, List<ErrorDetail> data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
