package com.equifax.c2o.api.ruleEngine.service;

import com.equifax.c2o.api.ruleEngine.businessRule.BusinessRule;
import com.equifax.c2o.api.ruleEngine.businessRule.MoveAccountValidate;
import com.equifax.c2o.api.ruleEngine.businessRule.MoveContractValidate;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final RuleConfigRepository ruleConfigRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private MoveAccountValidate moveAccountValidate;

    @Autowired
    private MoveContractValidate moveContractValidate;

    private Map<String, BusinessRule> ruleMap;

    @PostConstruct
    private void init() {
        ruleMap = new HashMap<>();
        ruleMap.put(moveAccountValidate.getRuleName(), moveAccountValidate);
        ruleMap.put(moveContractValidate.getRuleName(), moveContractValidate);
    }

    public void validateInputData(String ruleCode, JsonNode inputData) throws ValidationException {
        try {
            log.info("Starting validation for rule code: {}", ruleCode);
            
            if (ruleCode == null || ruleCode.trim().isEmpty()) {
                throw new ValidationException("Rule code is required");
            }
            
            if (inputData == null) {
                throw new ValidationException("Input data is required");
            }

            // Find rule configuration
            RuleConfig ruleConfig = ruleConfigRepository.findByRuleCode(ruleCode)
                    .orElseThrow(() -> new ValidationException("Rule code not found: " + ruleCode));

            // Schema validation
            List<String> schemaErrors = validateSchema(ruleCode, ruleConfig, inputData);
            if (!schemaErrors.isEmpty()) {
                String errorMessage = String.join("; ", schemaErrors);
                log.error("Schema validation failed for rule {}: {}", ruleCode, errorMessage);
                throw new ValidationException(errorMessage);
            }
            log.info("Schema validation successful for rule code: {}", ruleCode);

            // Business rule validation
            List<ErrorDetail> businessErrors = validateBusinessRule(ruleCode, inputData);
            if (!businessErrors.isEmpty()) {
                String errorMessage = businessErrors.stream()
                    .map(ErrorDetail::getMessage)
                    .collect(Collectors.joining("; "));
                log.error("Business validation failed for rule {}: {}", ruleCode, errorMessage);
                throw new ValidationException(errorMessage);
            }
            
            log.info("Validation completed successfully for rule code: {}", ruleCode);
            
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.error("Unexpected error during validation for rule {}: {}", ruleCode, e.getMessage(), e);
            throw new ValidationException("Validation failed: " + e.getMessage());
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

    private List<ErrorDetail> validateBusinessRule(String ruleCode, JsonNode inputData) {
        List<ErrorDetail> errors = new ArrayList<>();
        try {
            BusinessRule businessRule = getBusinessRule(ruleCode);
            if (businessRule != null) {
                // Convert JsonNode to JSON string properly using ObjectMapper
                String inputJson = objectMapper.writeValueAsString(inputData);
                errors = businessRule.validate(inputJson);
                
                if (!errors.isEmpty()) {
                    errors.forEach(error -> 
                        log.warn("Business validation error for rule {}: {} - {}", 
                            ruleCode, error.getCode(), error.getMessage())
                    );
                }
            } else {
                log.info("No business validation defined for rule code: {}", ruleCode);
            }
        } catch (Exception e) {
            log.error("Error during business validation for rule {}: {}", ruleCode, e.getMessage(), e);
            errors.add(new ErrorDetail("VALIDATION_ERROR", "Business validation failed: " + e.getMessage()));
        }
        return errors;
    }
    
    private BusinessRule getBusinessRule(String ruleCode) {
        if (ruleCode == null) return null;
        
        return ruleMap.get(ruleCode);
    }
}
