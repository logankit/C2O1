package com.equifax.c2o.api.RequestDetails.exception;

import java.util.List;

public class HeaderValidationException extends CustomException {
    private List<ValidationError> validationErrors;

    public HeaderValidationException(List<ValidationError> validationErrors) {
        super("EFX_HEADER_VALIDATION_FAILED", "Header validation failed");
        this.validationErrors = validationErrors;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }
}
