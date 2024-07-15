package com.equifax.c2o.api.RequestDetails.exception;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CustomHeaderValidationException extends RuntimeException {
    private final List<CustomException> validationErrors;

    public CustomHeaderValidationException(List<CustomException> validationErrors) {
        super("Header validation failed");
        this.validationErrors = validationErrors;
    }

    public List<CustomException> getValidationErrors() {
        return validationErrors;
    }

    public static class CustomException {
        private String code;
        private String message;

        public CustomException(String code, String message) {
            this.code = code;
            this.message = message;
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
    }
}
