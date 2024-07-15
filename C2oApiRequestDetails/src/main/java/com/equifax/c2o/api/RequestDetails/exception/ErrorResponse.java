package com.equifax.c2o.api.RequestDetails.exception;

import java.util.List;

public class ErrorResponse {
    private String code;
    private String message;
    private List<ValidationError> validationErrors;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String code, String message, List<ValidationError> validationErrors) {
        this.code = code;
        this.message = message;
        this.validationErrors = validationErrors;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
