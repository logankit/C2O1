package com.equifax.c2o.api.ruleEngine.service;

import com.equifax.c2o.api.ruleEngine.businessRule.BusinessRule;
import com.equifax.c2o.api.ruleEngine.businessRule.MoveAccountValidate;
import com.equifax.c2o.api.ruleEngine.entity.RuleConfig;
import com.equifax.c2o.api.ruleEngine.exception.ValidationException;
import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import com.equifax.c2o.api.ruleEngine.repository.RuleConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final RuleConfigRepository ruleConfigRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private MoveAccountValidate moveAccountValidate;

    public void validateInputData(String ruleCode, JsonNode inputData) throws Exception {
        log.info("Validating input data for rule code: {}", ruleCode);
        
        RuleConfig ruleConfig = ruleConfigRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ValidationException("Rule code not found: " + ruleCode));

        String schemaString = ruleConfig.getInputSchema();
        log.debug("Retrieved schema for rule code {}: {}", ruleCode, schemaString);

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaString);

        Set<ValidationMessage> validationResult = schema.validate(inputData);

        if (!validationResult.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ValidationMessage vm : validationResult) {
                errors.append(vm.getMessage()).append("; ");
                log.warn("Validation error for rule {}: {}", ruleCode, vm.getMessage());
            }
            throw new ValidationException(errors.toString());
        }

        log.info("Schema validation successful for rule code: {}", ruleCode);
        
        // Execute business rule validation
        BusinessRule businessRule = getBusinessRule(ruleCode);
        if (businessRule != null) {
            List<ErrorDetail> businessErrors = businessRule.validate(inputData.toString());
            if (!businessErrors.isEmpty()) {
                StringBuilder errors = new StringBuilder();
                for (ErrorDetail error : businessErrors) {
                    errors.append(error.getMessage()).append("; ");
                    log.warn("Business validation error for rule {}: {}", ruleCode, error.getMessage());
                }
                throw new ValidationException(errors.toString());
            }
        }
        
        log.info("Business validation successful for rule code: {}", ruleCode);
    }
    
    private BusinessRule getBusinessRule(String ruleCode) {
        switch (ruleCode) {
            case "MOVE_ACCOUNT_VALIDATE":
                return moveAccountValidate;
            default:
                log.warn("No business rule implementation found for rule code: {}", ruleCode);
                return null;
        }
    }
}
