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
        response.setCode(ex.getCode());
        response.setMessage(ex.getMessage());
        response.setData(ex.getData());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        ApiResponse response = new ApiResponse();
        response.setStatus(APIConstants.FAILED);
        response.setCode("EFX_C2O_ERR_RULE_EXEC_FAILED");
        response.setMessage("Rule execution failed");
        response.setData(Collections.singletonList(
            new ErrorDetail("EFX_C2O_ERR_UNEXPECTED", ex.getMessage(), null)
        ));
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
