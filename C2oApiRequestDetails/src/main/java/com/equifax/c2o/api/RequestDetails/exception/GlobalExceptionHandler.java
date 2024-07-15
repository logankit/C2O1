package com.equifax.c2o.api.RequestDetails.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HeaderValidationException.class)
    public ResponseEntity<ErrorResponse> handleHeaderValidationException(HeaderValidationException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getValidationErrors());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateClientCorrelationIdException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateClientCorrelationIdException(DuplicateClientCorrelationIdException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidSourceSystemException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSourceSystemException(InvalidSourceSystemException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidBusinessUnitException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBusinessUnitException(InvalidBusinessUnitException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCode(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
