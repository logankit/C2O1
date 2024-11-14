package com.equifax.c2o.api.ruleEngine.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
