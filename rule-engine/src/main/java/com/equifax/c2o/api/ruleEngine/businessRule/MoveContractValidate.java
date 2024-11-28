package com.equifax.c2o.api.ruleEngine.businessRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
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

    @PersistenceContext
    private EntityManager em;

    @Override
    public String getRuleName() {
        return "MOVE_CONTRACT_VALIDATE";
    }

    @Override
    public List<ErrorDetail> validate(Object inputData) throws Exception {
        log.info("Starting validation for MOVE_CONTRACT_VALIDATE");
        ObjectMapper mapper = new ObjectMapper();
        MoveContractRequest requestInput = mapper.readValue((String)inputData, MoveContractRequest.class);
        log.info("Received request: sourceContractId={}, targetContractId={}", 
                requestInput.getSourceContractId(), requestInput.getTargetContractId());

        List<ErrorDetail> retVal = new ArrayList<ErrorDetail>();
        boolean hasValidationErrors = false;

        // Validate required fields
        if (requestInput.getSourceContractId() == null || requestInput.getTargetContractId() == null) {
            log.error("Source or Target Contract ID is missing");
            retVal.add(new ErrorDetail("EFX_MISSING_CONTRACT_ID", "Source and Target Contract IDs are required"));
            return retVal; // Return immediately for null values as we can't proceed
        }

        // Validate contracts are different
        if (requestInput.getSourceContractId().equals(requestInput.getTargetContractId())) {
            log.error("Source and Target Contract IDs are the same: {}", requestInput.getSourceContractId());
            retVal.add(new ErrorDetail("EFX_SAME_CONTRACT", "Source and Target Contract IDs cannot be the same"));
            hasValidationErrors = true;
        }

        if (!hasValidationErrors) {
            // Query to validate contracts exist and are latest versions
            String latestContractQuery = 
                "SELECT c.contract_id, " +
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

            // Check if both contracts exist
            if (contracts.size() != 2) {
                log.error("One or both contracts not found");
                retVal.add(new ErrorDetail("EFX_CONTRACT_NOT_FOUND", 
                    "One or both contracts not found"));
                hasValidationErrors = true;
            }

            if (!hasValidationErrors) {
                // Check if both contracts are latest version
                for (Object[] contract : contracts) {
                    Long contractId = ((Number) contract[0]).longValue();
                    Long maxContractId = ((Number) contract[1]).longValue();
                    
                    if (!contractId.equals(maxContractId)) {
                        String contractType = contractId.equals(Long.valueOf(requestInput.getSourceContractId())) 
                            ? "Source" : "Target";
                        
                        log.error("{} Contract {} is not the latest version. Latest version is {}", 
                            contractType, contractId, maxContractId);
                        
                        retVal.add(new ErrorDetail("EFX_NOT_LATEST_CONTRACT", 
                            contractType + " Contract " + contractId + " is not the latest version. " +
                            "Latest Contract ID is " + maxContractId));
                        hasValidationErrors = true;
                    }
                }
            }

            if (!hasValidationErrors) {
                // Query to get business units for both contracts
                String buQuery = 
                    "SELECT c.contract_id, cbi.bu_id " +
                    "FROM c2o_contract c, c2o_contract_bu_intr cbi " +
                    "WHERE c.contract_id = cbi.contract_id " +
                    "AND c.contract_id IN (:sourceContractId, :targetContractId)";

                log.debug("Executing business unit query: {}", buQuery);
                Query buQueryObj = em.createNativeQuery(buQuery);
                buQueryObj.setParameter("sourceContractId", requestInput.getSourceContractId());
                buQueryObj.setParameter("targetContractId", requestInput.getTargetContractId());

                List<Object[]> buResults = buQueryObj.getResultList();

                if (buResults.size() != 2) {
                    log.error("Business unit information not found for one or both contracts");
                    retVal.add(new ErrorDetail("EFX_BU_NOT_FOUND", 
                        "Business unit information not found for one or both contracts"));
                    hasValidationErrors = true;
                }

                if (!hasValidationErrors) {
                    String sourceBuId = null;
                    String targetBuId = null;

                    for (Object[] result : buResults) {
                        Long contractId = ((Number) result[0]).longValue();
                        String buId = (String) result[1];

                        if (contractId.equals(Long.valueOf(requestInput.getSourceContractId()))) {
                            sourceBuId = buId;
                        } else {
                            targetBuId = buId;
                        }
                    }

                    if (!sourceBuId.equals(targetBuId)) {
                        log.error("Contracts belong to different business units. Source BU: {}, Target BU: {}", 
                            sourceBuId, targetBuId);
                        retVal.add(new ErrorDetail("EFX_DIFFERENT_BUSINESS_UNIT", 
                            "Source Contract " + requestInput.getSourceContractId() + " belongs to Business Unit " + sourceBuId + 
                            " and Target Contract " + requestInput.getTargetContractId() + " belongs to Business Unit " + targetBuId + 
                            ". Contracts must belong to the same Business Unit."));
                        hasValidationErrors = true;
                    }
                }
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
