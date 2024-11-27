package com.equifax.c2o.api.ruleEngine.businessRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import com.equifax.c2o.api.ruleEngine.model.moveaccount.types.MoveAccountRequest;
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

    private static final String VERSION_STATUS_LATEST = "LATEST";
    private static final String RECORD_STATUS_ACTIVE = "1";

    @Override
    public String getRuleName() {
        return "MOVE_CONTRACT_VALIDATE";
    }

    @Override
    public List<ErrorDetail> validate(Object inputData) throws Exception {
        log.info("Starting validation for MOVE_CONTRACT_VALIDATE");
        ObjectMapper mapper = new ObjectMapper();
        MoveAccountRequest requestInput = mapper.readValue((String)inputData, MoveAccountRequest.class);
        log.info("Received request: sourceContractId={}, targetContractId={}", 
                requestInput.getSourceContractId(), requestInput.getTargetContractId());

        List<ErrorDetail> retVal = new ArrayList<ErrorDetail>();

        // Validate required fields
        if (requestInput.getSourceContractId() == null || requestInput.getTargetContractId() == null) {
            log.error("Source or Target Contract ID is missing");
            retVal.add(new ErrorDetail("EFX_MISSING_CONTRACT_ID", "Source and Target Contract IDs are required"));
            return retVal;
        }

        // Validate contracts are different
        if (requestInput.getSourceContractId().equals(requestInput.getTargetContractId())) {
            log.error("Source and Target Contract IDs are the same: {}", requestInput.getSourceContractId());
            retVal.add(new ErrorDetail("EFX_SAME_CONTRACT", "Source and Target Contract IDs cannot be the same"));
            return retVal;
        }

        // Validate both contracts exist and are latest versions
        String latestContractQuery = "SELECT c.contract_id, c.business_unit, c.version_status "
            + "FROM c2o_contract c "
            + "WHERE c.record_status = :recordStatus "
            + "AND c.contract_id IN (:sourceContractId, :targetContractId)";

        log.debug("Executing latest contract query: {}", latestContractQuery);
        log.debug("Query parameters - sourceContractId: {}, targetContractId: {}", 
                requestInput.getSourceContractId(), requestInput.getTargetContractId());

        Query query = em.createNativeQuery(latestContractQuery);
        query.setParameter("recordStatus", RECORD_STATUS_ACTIVE);
        query.setParameter("sourceContractId", requestInput.getSourceContractId());
        query.setParameter("targetContractId", requestInput.getTargetContractId());

        List<Object[]> contracts = query.getResultList();
        
        // Check if both contracts exist
        if (contracts.size() != 2) {
            log.error("One or both contracts not found or inactive");
            retVal.add(new ErrorDetail("EFX_CONTRACT_NOT_FOUND", 
                "One or both contracts not found or inactive"));
            return retVal;
        }

        // Check if both contracts are latest version
        boolean allLatest = contracts.stream()
            .allMatch(contract -> VERSION_STATUS_LATEST.equals(contract[2]));
        
        if (!allLatest) {
            log.error("One or both contracts are not the latest version");
            retVal.add(new ErrorDetail("EFX_NOT_LATEST_CONTRACT", 
                "Both source and target contracts must be the latest version"));
            return retVal;
        }

        // Check if contracts belong to same business unit
        String sourceBU = null;
        String targetBU = null;
        
        for (Object[] contract : contracts) {
            Long contractId = ((Number) contract[0]).longValue();
            String businessUnit = (String) contract[1];
            
            if (contractId.equals(Long.valueOf(requestInput.getSourceContractId()))) {
                sourceBU = businessUnit;
            } else {
                targetBU = businessUnit;
            }
        }

        if (sourceBU == null || targetBU == null || !sourceBU.equals(targetBU)) {
            log.error("Contracts belong to different business units. Source BU: {}, Target BU: {}", 
                    sourceBU, targetBU);
            retVal.add(new ErrorDetail("EFX_DIFFERENT_BUSINESS_UNIT", 
                "Source and target contracts must belong to the same business unit"));
            return retVal;
        }

        log.info("Contract validation completed successfully");
        return retVal;
    }
}
