package com.equifax.c2o.api.RequestDetails.exception;

import java.util.List;

public class CommonValidationException extends RuntimeException {
    private String code;
    private List<ValidationError> validationErrors;

    public CommonValidationException(String code, String message, List<ValidationError> validationErrors) {
        super(message);
        this.code = code;
        this.validationErrors = validationErrors;
    }

    public String getCode() {
        return code;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
