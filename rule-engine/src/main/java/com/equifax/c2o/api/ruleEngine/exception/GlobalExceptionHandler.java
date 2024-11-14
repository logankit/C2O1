package com.equifax.c2o.api.ruleEngine.exception;

import com.equifax.c2o.api.ruleEngine.constants.APIConstants;
import com.equifax.c2o.api.ruleEngine.model.ApiResponse;
import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse> handleValidationException(ValidationException ex) {
        ApiResponse response = new ApiResponse();
        response.setStatus(APIConstants.FAILED);
        response.setMessage(APIConstants.VALIDATION_FAILED);
        ErrorDetail error = new ErrorDetail("VALIDATION_ERROR", ex.getMessage());
        response.setData(Collections.singletonList(error));
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        ApiResponse response = new ApiResponse();
        response.setStatus(APIConstants.FAILED);
        response.setMessage(APIConstants.PROCESSING_ERROR);
        ErrorDetail error = new ErrorDetail("SERVER_ERROR", ex.getMessage());
        response.setData(Collections.singletonList(error));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
