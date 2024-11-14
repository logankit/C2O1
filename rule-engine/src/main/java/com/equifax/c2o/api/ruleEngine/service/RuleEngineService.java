package com.equifax.c2o.api.ruleEngine.service;

import com.equifax.c2o.api.ruleEngine.entity.RuleConfig;
import com.equifax.c2o.api.ruleEngine.exception.ValidationException;
import com.equifax.c2o.api.ruleEngine.repository.RuleConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RuleEngineService {

    private final RuleConfigRepository ruleConfigRepository;
    private final ObjectMapper objectMapper;

    public void validateInputData(String ruleCode, Object inputData) throws Exception {
        RuleConfig ruleConfig = ruleConfigRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ValidationException("Rule code not found: " + ruleCode));

        String schemaString = ruleConfig.getInputSchema();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaString);

        JsonNode inputNode = objectMapper.valueToTree(inputData);
        Set<ValidationMessage> validationResult = schema.validate(inputNode);

        if (!validationResult.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            for (ValidationMessage vm : validationResult) {
                errors.append(vm.getMessage()).append("; ");
            }
            throw new ValidationException(errors.toString());
        }
    }
}
