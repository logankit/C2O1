package com.equifax.c2o.api.RequestDetails.exception;

public class ValidationError {
    private String code;
    private String message;

    public ValidationError(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
