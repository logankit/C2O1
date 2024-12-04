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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

    @Value("${validation.thread.pool.size:10}")
    private int threadPoolSize;

    private ThreadPoolExecutor validationExecutor;

    @PostConstruct
    private void init() {
        // Initialize rule map
        ruleMap = new HashMap<>();
        ruleMap.put(moveAccountValidate.getRuleName(), moveAccountValidate);
        ruleMap.put(moveContractValidate.getRuleName(), moveContractValidate);
        ruleMap.put(moveAccountGroupValidate.getRuleName(), moveAccountGroupValidate);
        
        // Initialize thread pool with monitoring
        validationExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
        log.info("Initialized validation thread pool with size: {}", threadPoolSize);
        
        // Log thread pool stats periodically
        CompletableFuture.runAsync(() -> {
            while (!validationExecutor.isShutdown()) {
                try {
                    Thread.sleep(60000); // Log every minute
                    logThreadPoolStats();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void logThreadPoolStats() {
        log.info("Thread Pool Stats - Active: {}, Pool: {}, Queue: {}, Completed: {}",
            validationExecutor.getActiveCount(),
            validationExecutor.getPoolSize(),
            validationExecutor.getQueue().size(),
            validationExecutor.getCompletedTaskCount()
        );
    }
    
    @PreDestroy
    private void cleanup() {
        if (validationExecutor != null) {
            logThreadPoolStats(); // Log final stats
            validationExecutor.shutdown();
            try {
                if (!validationExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    validationExecutor.shutdownNow();
                }
                log.info("Validation thread pool shutdown completed");
            } catch (InterruptedException e) {
                validationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("Validation thread pool shutdown interrupted");
            }
        }
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

            // Business rule validation - now parallel
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

        // Get all applicable business rules
        List<BusinessRule> rules = new ArrayList<>();
        BusinessRule rule = getBusinessRule(ruleCode);
        if (rule != null) {
            rules.add(rule);
        }

        if (rules.isEmpty()) {
            log.info("No business rules found for rule code: {}", ruleCode);
            return allErrors;
        }

        // Create CompletableFuture for each validation rule
        List<CompletableFuture<List<ErrorDetail>>> futures = rules.stream()
            .map(businessRule -> CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("Starting validation for rule: {}", businessRule.getRuleName());
                    List<ErrorDetail> errors = businessRule.validate(inputJson);
                    if (!errors.isEmpty()) {
                        log.warn("Found {} validation errors for rule: {}", 
                            errors.size(), businessRule.getRuleName());
                    }
                    return errors;
                } catch (Exception e) {
                    log.error("Error in async validation for rule {}: {}", 
                        businessRule.getRuleName(), e.getMessage(), e);
                    List<ErrorDetail> asyncErrors = new ArrayList<>();
                    asyncErrors.add(new ErrorDetail(
                        "EFX_C2O_ERR_RULE_EXEC_FAILED",
                        String.format("Validation failed for rule %s: %s", 
                            businessRule.getRuleName(), e.getMessage()),
                        null
                    ));
                    return asyncErrors;
                }
            }, validationExecutor))
            .collect(Collectors.toList());

        try {
            // Wait for all validations to complete with timeout
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            // Wait for completion or timeout
            allOf.get(30, TimeUnit.SECONDS);

            // Collect all errors
            futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        log.error("Error getting validation results: {}", e.getMessage());
                        List<ErrorDetail> errors = new ArrayList<>();
                        errors.add(new ErrorDetail(
                            "EFX_C2O_ERR_RESULT_COLLECTION",
                            "Failed to collect validation results: " + e.getMessage(),
                            null
                        ));
                        return errors;
                    }
                })
                .forEach(allErrors::addAll);

        } catch (java.util.concurrent.TimeoutException te) {
            log.error("Validation timed out for rule code {}", ruleCode);
            // Cancel all futures on timeout
            futures.forEach(future -> future.cancel(true));
            allErrors.add(new ErrorDetail(
                "EFX_C2O_ERR_RULE_TIMEOUT",
                String.format("Validation timed out after 30 seconds for rule code %s", ruleCode),
                null
            ));
        } catch (Exception e) {
            log.error("Error during parallel validation for rule {}: {}", ruleCode, e.getMessage(), e);
            // Cancel any remaining futures
            futures.forEach(future -> future.cancel(true));
            allErrors.add(new ErrorDetail(
                "EFX_C2O_ERR_PARALLEL_EXECUTION",
                String.format("Error during parallel validation for rule %s: %s", ruleCode, e.getMessage()),
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
