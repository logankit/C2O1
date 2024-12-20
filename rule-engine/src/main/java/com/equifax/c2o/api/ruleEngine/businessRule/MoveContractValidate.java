package com.equifax.c2o.api.ruleEngine.businessRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import com.equifax.c2o.api.ruleEngine.model.EntityType;
import com.equifax.c2o.api.ruleEngine.model.movecontract.types.MoveContractRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MoveContractValidate extends BusinessRule {

    private static final int COMPLETED_STATUS = 6;

    @PersistenceContext
    private EntityManager em;

    @Override
    public String getRuleName() {
        return "MOVE_ACCOUNT_CONTRACT_VALIDATE";
    }

    @Override
    public List<ErrorDetail> validate(Object inputData) throws Exception {
        log.info("Starting validation for MOVE_ACCOUNT_CONTRACT_VALIDATE");
        ObjectMapper mapper = new ObjectMapper();
        MoveContractRequest requestInput;
        
        if (inputData instanceof String) {
            requestInput = mapper.readValue((String)inputData, MoveContractRequest.class);
        } else {
            requestInput = mapper.convertValue(inputData, MoveContractRequest.class);
        }
        
        log.info("Received request: sourceContractId={}, targetContractId={}", 
                requestInput.getSourceContractId(), requestInput.getTargetContractId());

        List<ErrorDetail> retVal = new ArrayList<>();
        boolean hasValidationErrors = false;

        // Validate required fields
        if (requestInput.getSourceContractId() == null || requestInput.getTargetContractId() == null) {
            String missingContract = requestInput.getSourceContractId() == null ? "Source" : "Target";
            log.error("{} Contract ID is missing", missingContract);
            retVal.add(new ErrorDetail(
                "EFX_C2O_ERR_MISSING_CONTRACT_ID", 
                missingContract + " Contract ID is required", 
                EntityType.TRG_CONTRACT_ID.name() + "[null]"
            ));
            return retVal;
        }

        // Query to validate contracts exist and are latest versions and completed status
        String latestContractQuery = 
            "SELECT DISTINCT c.contract_id, " +
            "       c.status, " +
            "       (SELECT MAX(contract_id) " +
            "        FROM c2o_contract " +
            "        WHERE root_contract_id = c.root_contract_id) as max_contract_id " +
            "FROM c2o_contract c " +
            "WHERE c.contract_id IN (:sourceContractId, :targetContractId)";

        log.debug("Executing latest contract query: {}", latestContractQuery);
        Query query = em.createNativeQuery(latestContractQuery);
        query.setParameter("sourceContractId", requestInput.getSourceContractId());
        query.setParameter("targetContractId", requestInput.getTargetContractId());

        List<Object[]> contracts = query.getResultList();

        // Check if contracts exist
        int expectedContractCount = requestInput.getSourceContractId().equals(requestInput.getTargetContractId()) ? 1 : 2;
        if (contracts.size() != expectedContractCount) {
            log.error("One or both contracts not found");
            List<Long> foundContractIds = contracts.stream()
                .map(contract -> ((Number) contract[0]).longValue())
                .collect(Collectors.toList());
            
            if (!foundContractIds.contains(requestInput.getSourceContractId().longValue())) {
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_CONTRACT_NOT_FOUND", 
                    "Source contract not found", 
                    EntityType.TRG_CONTRACT_ID.name() + "[" + requestInput.getSourceContractId().toString() + "]"
                ));
            }
            if (!foundContractIds.contains(requestInput.getTargetContractId().longValue())) {
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_CONTRACT_NOT_FOUND", 
                    "Target contract not found", 
                    EntityType.TRG_CONTRACT_ID.name() + "[" + requestInput.getTargetContractId().toString() + "]"
                ));
            }
            hasValidationErrors = true;
        } else {
            // Check if contracts are latest version and completed status
            for (Object[] contract : contracts) {
                Long contractId = ((Number) contract[0]).longValue();
                Number status = (Number) contract[1];
                Long maxContractId = ((Number) contract[2]).longValue();
                
                String contractType = contractId.equals(requestInput.getSourceContractId().longValue()) 
                    ? "Source" : "Target";
                
                // Check if contract is not completed (status != 6)
                if (status == null || status.intValue() != 6) {
                    log.error("{} Contract {} is not in completed status. Current status: {}", 
                        contractType, contractId, status);
                    
                    retVal.add(new ErrorDetail(
                        "EFX_C2O_ERR_CONTRACT_NOT_COMPLETED", 
                        contractType + " Contract " + contractId + " is not in completed status",
                        EntityType.TRG_CONTRACT_ID.name() + "[" + contractId.toString() + "]"
                    ));
                    hasValidationErrors = true;
                }
                
                // Check if contract is latest version
                if (!contractId.equals(maxContractId)) {
                    log.error("{} Contract {} is not the latest version. Latest version is {}", 
                        contractType, contractId, maxContractId);
                    
                    retVal.add(new ErrorDetail(
                        "EFX_C2O_ERR_NOT_LATEST_CONTRACT", 
                        contractType + " Contract " + contractId + " is not the latest version. " +
                        "Latest Contract ID is " + maxContractId,
                        EntityType.TRG_CONTRACT_ID.name() + "[" + contractId.toString() + "]"
                    ));
                    hasValidationErrors = true;
                }
            }
        }

        // Query to get business units for contracts
        String buQuery = 
            "SELECT DISTINCT c.contract_id, cbi.bu_id " +
            "FROM c2o_contract c " +
            "JOIN c2o_contract_bu_intr cbi ON c.contract_id = cbi.contract_id " +
            "WHERE c.contract_id IN (:sourceContractId, :targetContractId)";

        log.debug("Executing business unit query: {}", buQuery);
        Query buQueryObj = em.createNativeQuery(buQuery);
        buQueryObj.setParameter("sourceContractId", requestInput.getSourceContractId());
        buQueryObj.setParameter("targetContractId", requestInput.getTargetContractId());

        List<Object[]> buResults = buQueryObj.getResultList();

        // For same contract case, we expect 1 result, for different contracts we expect 2
        if (buResults.size() != expectedContractCount) {
            log.error("Business unit information not found for one or both contracts");
            retVal.add(new ErrorDetail("EFX_C2O_ERR_BU_NOT_FOUND", 
                "Business unit information not found for one or both contracts",
                EntityType.TRG_CONTRACT_ID.name() + "[" + requestInput.getSourceContractId().toString() + "]"
            ));
            hasValidationErrors = true;
        } else {
            String sourceBuId = null;
            String targetBuId = null;

            for (Object[] result : buResults) {
                Long contractId = ((Number) result[0]).longValue();
                String buId = (String) result[1];
                
                if (contractId.equals(requestInput.getSourceContractId().longValue())) {
                    sourceBuId = buId;
                } 
                if (contractId.equals(requestInput.getTargetContractId().longValue())) {
                    targetBuId = buId;
                }
            }

            // Check if either BU is null
            if (sourceBuId == null || targetBuId == null) {
                String missingBuContract = sourceBuId == null ? "Source" : "Target";
                log.error("Business unit not found for {} contract", missingBuContract);
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_BU_NOT_FOUND", 
                    "Business unit not found for " + missingBuContract + " contract",
                    EntityType.TRG_CONTRACT_ID.name() + "[" + 
                        (sourceBuId == null ? requestInput.getSourceContractId() : requestInput.getTargetContractId()) + "]"
                ));
                hasValidationErrors = true;
            } else if (!sourceBuId.equals(targetBuId) && !requestInput.getSourceContractId().equals(requestInput.getTargetContractId())) {
                // Only check BU mismatch for different contracts
                log.error("Business units do not match: source={}, target={}", sourceBuId, targetBuId);
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_BU_MISMATCH", 
                    "Source contract business unit (" + sourceBuId + 
                    ") does not match target contract business unit (" + targetBuId + ")",
                    EntityType.TRG_CONTRACT_ID.name() + "[" + requestInput.getSourceContractId() + "]"
                ));
                hasValidationErrors = true;
            }
        }

        if (!hasValidationErrors) {
            log.info("Contract validation completed successfully");
        } else {
            log.error("Contract validation failed with {} errors", retVal.size());
        }
        
        return retVal;
    }
}
