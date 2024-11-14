package com.equifax.c2o.api.ruleEngine.controller;

import com.equifax.c2o.api.ruleEngine.constants.APIConstants;
import com.equifax.c2o.api.ruleEngine.model.ApiResponse;
import com.equifax.c2o.api.ruleEngine.model.RequestPayload;
import com.equifax.c2o.api.ruleEngine.service.RuleEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/v1/execute")
@RequiredArgsConstructor
public class RuleEngineController {

    private final RuleEngineService ruleEngineService;

    @PostMapping
    public ResponseEntity<ApiResponse> executeRule(
            @RequestHeader(name = "efx-client-correlation-id") UUID correlationId,
            @Valid @RequestBody RequestPayload requestPayload) {

        ruleEngineService.validateInputData(requestPayload.getRuleCode(), requestPayload.getInputData());

        ApiResponse response = new ApiResponse();
        response.setStatus(APIConstants.SUCCESS);
        response.setMessage("Validation successful.");
        response.setData(Collections.emptyList());

        return ResponseEntity.ok(response);
    }
}