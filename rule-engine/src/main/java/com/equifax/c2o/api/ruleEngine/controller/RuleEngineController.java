package com.equifax.c2o.api.ruleEngine.controller;

import com.equifax.c2o.api.ruleEngine.constants.APIConstants;
import com.equifax.c2o.api.ruleEngine.model.ApiResponse;
import com.equifax.c2o.api.ruleEngine.model.RequestPayload;
import com.equifax.c2o.api.ruleEngine.service.RuleEngineService;
import com.equifax.c2o.api.ruleEngine.exception.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;

    @PostMapping("/v1/execute")
    public ResponseEntity<ApiResponse> executeRule(
            @RequestHeader(name = "efx-client-correlation-id") UUID correlationId,
            @Valid @RequestBody RequestPayload requestPayload) {
        try {
            ruleEngineService.validateInputData(requestPayload.getRuleCode(), requestPayload.getInputData());

            ApiResponse response = new ApiResponse();
            response.setStatus(APIConstants.SUCCESS);
            response.setMessage("Validation successful.");
            response.setData(Collections.emptyList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse();
            response.setStatus(APIConstants.FAILED);
            if (e instanceof ValidationException validationException) {
                response.setCode(validationException.getCode());
                response.setMessage(e.getMessage());
                response.setData(validationException.getData());
            } else {
                response.setMessage("Validation failed: " + e.getMessage());
                response.setData(Collections.emptyList());
            }
            return ResponseEntity.badRequest().body(response);
        }
    }
	
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        ApiResponse response = new ApiResponse();
        response.setStatus(APIConstants.SUCCESS);
        response.setMessage("Service is healthy");
        response.setData(Collections.emptyList());
        return ResponseEntity.ok(response);
    }
}
