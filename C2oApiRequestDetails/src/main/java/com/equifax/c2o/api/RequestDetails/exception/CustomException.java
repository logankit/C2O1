package com.equifax.c2o.api.RequestDetails.exception;

public abstract class CustomException extends RuntimeException {
    private String code;

    public CustomException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
