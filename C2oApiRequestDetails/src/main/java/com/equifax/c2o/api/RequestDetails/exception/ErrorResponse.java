package com.equifax.c2o.api.RequestDetails.exception;

import java.util.List;

public class ErrorResponse {
    private String code;
    private String message;
    private List<CustomHeaderValidationException.CustomException> validationErrors;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String message, List<CustomHeaderValidationException.CustomException> validationErrors) {
        this.code = "HEADER_VALIDATION_ERROR";
        this.message = message;
        this.validationErrors = validationErrors;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<CustomHeaderValidationException.CustomException> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<CustomHeaderValidationException.CustomException> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
