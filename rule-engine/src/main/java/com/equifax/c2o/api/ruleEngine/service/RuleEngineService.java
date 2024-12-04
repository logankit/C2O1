package com.equifax.c2o.api.ruleEngine.service;

import com.equifax.c2o.api.ruleEngine.businessRule.BusinessRule;
import com.equifax.c2o.api.ruleEngine.businessRule.MoveAccountValidate;
import com.equifax.c2o.api.ruleEngine.businessRule.MoveContractValidate;
import com.equifax.c2o.api.ruleEngine.businessRule.MoveAccountGroupValidate;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {

    private final RuleConfigRepository ruleConfigRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private MoveAccountValidate moveAccountValidate;

    @Autowired
    private MoveContractValidate moveContractValidate;

    @Autowired
    private MoveAccountGroupValidate moveAccountGroupValidate;

    private Map<String, BusinessRule> ruleMap;

    @PostConstruct
    private void init() {
        // Initialize rule map
        ruleMap = new HashMap<>();
        ruleMap.put(moveAccountValidate.getRuleName(), moveAccountValidate);
        ruleMap.put(moveContractValidate.getRuleName(), moveContractValidate);
        ruleMap.put(moveAccountGroupValidate.getRuleName(), moveAccountGroupValidate);
        log.info("Initialized rule engine service with {} rules", ruleMap.size());
    }

    public void validateInputData(String ruleCode, JsonNode inputData) throws ValidationException {
        try {
            log.info("Starting validation for rule code: {}", ruleCode);
            
            if (ruleCode == null || ruleCode.trim().isEmpty()) {
                throw new ValidationException("EFX_C2O_ERR_RULE_EXEC_FAILED", "Rule code is required", null);
            }
            
            if (inputData == null) {
                throw new ValidationException("EFX_C2O_ERR_RULE_EXEC_FAILED", "Input data is required", null);
            }

            // Find rule configuration
            RuleConfig ruleConfig = ruleConfigRepository.findByRuleCode(ruleCode)
                    .orElseThrow(() -> new ValidationException("EFX_C2O_ERR_RULE_EXEC_FAILED", 
                        "Rule code not found: " + ruleCode, null));

            // Schema validation
            List<String> schemaErrors = validateSchema(ruleCode, ruleConfig, inputData);
            if (!schemaErrors.isEmpty()) {
                String errorMessage = String.join("; ", schemaErrors);
                log.error("Schema validation failed for rule {}: {}", ruleCode, errorMessage);
                throw new ValidationException("EFX_C2O_ERR_RULE_EXEC_FAILED", 
                    "Schema validation failed", Collections.singletonList(
                        new ErrorDetail("EFX_C2O_ERR_SCHEMA_VALIDATION", errorMessage, null)));
            }
            log.info("Schema validation successful for rule code: {}", ruleCode);

            // Business rule validation
            List<ErrorDetail> businessErrors = validateBusinessRules(ruleCode, inputData);
            if (!businessErrors.isEmpty()) {
                log.error("Business validation failed for rule {}", ruleCode);
                throw new ValidationException("EFX_C2O_ERR_RULE_EXEC_FAILED", 
                    "Rule execution failed", businessErrors);
            }
            
            log.info("Validation completed successfully for rule code: {}", ruleCode);
            
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.error("Unexpected error during validation for rule {}: {}", ruleCode, e.getMessage(), e);
            throw new ValidationException("EFX_C2O_ERR_RULE_EXEC_FAILED", 
                "Validation failed: " + e.getMessage(), 
                Collections.singletonList(new ErrorDetail("EFX_C2O_ERR_UNEXPECTED", e.getMessage(), null)));
        }
    }

    private List<String> validateSchema(String ruleCode, RuleConfig ruleConfig, JsonNode inputData) {
        List<String> errors = new ArrayList<>();
        try {
            String schemaString = ruleConfig.getInputSchema();
            log.debug("Retrieved schema for rule code {}: {}", ruleCode, schemaString);

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaString);

            Set<ValidationMessage> validationResult = schema.validate(inputData);
            if (!validationResult.isEmpty()) {
                for (ValidationMessage vm : validationResult) {
                    String error = vm.getMessage();
                    errors.add(error);
                    log.warn("Schema validation error for rule {}: {}", ruleCode, error);
                }
            }
        } catch (Exception e) {
            log.error("Error during schema validation for rule {}: {}", ruleCode, e.getMessage(), e);
            errors.add("Schema validation failed: " + e.getMessage());
        }
        return errors;
    }

    private List<ErrorDetail> validateBusinessRules(String ruleCode, JsonNode inputData) {
        List<ErrorDetail> allErrors = new ArrayList<>();
        String inputJson;
        try {
            inputJson = objectMapper.writeValueAsString(inputData);
        } catch (Exception e) {
            log.error("Error converting input data to JSON for rule {}: {}", ruleCode, e.getMessage());
            allErrors.add(new ErrorDetail(
                "EFX_C2O_ERR_INPUT_CONVERSION",
                "Failed to convert input data to JSON: " + e.getMessage(),
                null
            ));
            return allErrors;
        }

        BusinessRule rule = getBusinessRule(ruleCode);
        if (rule == null) {
            log.info("No business rule found for rule code: {}", ruleCode);
            return allErrors;
        }

        try {
            log.debug("Starting validation for rule: {}", rule.getRuleName());
            List<ErrorDetail> errors = rule.validate(inputJson);
            if (!errors.isEmpty()) {
                log.warn("Found {} validation errors for rule: {}", 
                    errors.size(), rule.getRuleName());
                allErrors.addAll(errors);
            }
        } catch (Exception e) {
            log.error("Error in validation for rule {}: {}", 
                rule.getRuleName(), e.getMessage(), e);
            allErrors.add(new ErrorDetail(
                "EFX_C2O_ERR_RULE_EXEC_FAILED",
                String.format("Validation failed for rule %s: %s", 
                    rule.getRuleName(), e.getMessage()),
                null
            ));
        }

        return allErrors;
    }

    private BusinessRule getBusinessRule(String ruleCode) {
        if (ruleCode == null) return null;
        return ruleMap.get(ruleCode);
    }
}
