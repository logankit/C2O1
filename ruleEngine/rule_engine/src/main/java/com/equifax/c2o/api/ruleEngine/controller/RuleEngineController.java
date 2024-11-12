
package com.equifax.c2o.api.ruleEngine.controller;

import com.equifax.c2o.api.ruleEngine.model.EfxRequest;
import com.equifax.c2o.api.ruleEngine.model.EfxResponse;
import com.equifax.c2o.api.ruleEngine.service.RuleEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class RuleEngineController {

    @Autowired
    private RuleEngineService ruleEngineService;

    @PostMapping("/execute")
    public ResponseEntity<EfxResponse> execute(
            @RequestHeader(value = "efx-client-correlation-id", required = true) UUID correlationId,
            @RequestBody EfxRequest request
    ) {
        EfxResponse response = ruleEngineService.processRequest(request, correlationId);
        HttpStatus status = response.getStatus().equals("SUCCESS") ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(response, status);
    }
}
