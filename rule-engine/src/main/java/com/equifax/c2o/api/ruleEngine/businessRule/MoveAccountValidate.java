package com.equifax.c2o.api.ruleEngine.businessRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import com.equifax.c2o.api.ruleEngine.model.moveaccount.types.MoveAccountRequest;
import com.equifax.c2o.api.ruleEngine.model.moveaccount.types.ShipTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MoveAccountValidate extends BusinessRule {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String getRuleName() {
        return "MOVE_ACCOUNT_VALIDATE";
    }

    @Override
    public List<ErrorDetail> validate(Object inputData) throws Exception {
        log.info("Starting validation for MOVE_ACCOUNT_VALIDATE");
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

        List<String> sourceShiptos = new ArrayList<String>();
        List<String> targetBillTos = new ArrayList<String>();
        if(requestInput.getShipTos() != null && !requestInput.getShipTos().isEmpty()) {
            requestInput.getShipTos().forEach(shipto -> {
                if (shipto.getObaNumbers() != null) {
                    sourceShiptos.addAll(shipto.getObaNumbers());
                }
                if (shipto.getTargetBillTo() != null) {
                    targetBillTos.add(shipto.getTargetBillTo());
                }
            });
            log.info("Processing shipTos - Source ShipTos: {}, Target BillTos: {}", 
                    sourceShiptos, targetBillTos);
        }

        List<String> sourceBillTos = new ArrayList<String>();
        if(requestInput.getBillTos() != null && !requestInput.getBillTos().isEmpty()) {
            sourceBillTos.addAll(requestInput.getBillTos());
            log.info("Processing billTos - Source BillTos: {}", sourceBillTos);
        }

        // Skip validations if no accounts to process
        if (sourceShiptos.isEmpty() && sourceBillTos.isEmpty()) {
            log.warn("No accounts to process in the request");
            retVal.add(new ErrorDetail("EFX_NO_ACCOUNTS", "No accounts provided for processing"));
            return retVal;
        }

        // validate source shiptos are active
        if (!sourceShiptos.isEmpty()) {
            String queryStr = "Select a.oba_number from c2o_account a where a.account_version = 'CURRENT' "
                + " and (a.account_status != '27' AND a.account_type = 'SHIP-TO') "
                + " and a.oba_number in :p_oba_list ";
            log.debug("Executing active shiptos query: {}", queryStr);
            log.debug("Query parameters - p_oba_list: {}", sourceShiptos);
            
            Query query = em.createNativeQuery(queryStr);
            query.setParameter("p_oba_list", sourceShiptos);
            List<?> inactiveShiptos = query.getResultList();
            log.info("Found {} inactive shiptos", inactiveShiptos.size());
            
            inactiveShiptos.forEach(acc -> {
                String obaNumber = (String)acc;
                log.warn("Inactive shipto found: {}", obaNumber);
                ErrorDetail error = new ErrorDetail("EFX_NOT_AN_ACTIVE_SHIPTO", 
                    obaNumber + " is not an active Shipto");
                retVal.add(error);
            });
        }

        // moving to a different BDOM
        if(!sourceShiptos.isEmpty() && !targetBillTos.isEmpty()) {
            requestInput.getShipTos().forEach(shipto -> {
                if (shipto.getObaNumbers() != null && !shipto.getObaNumbers().isEmpty() 
                    && shipto.getTargetBillTo() != null) {
                    String queryStr2 = "Select a.oba_number from c2o_account a, c2o_account b, c2o_account t "
                        + " where a.account_version = 'CURRENT' "
                        + " and a.oba_number in :p_oba_list "
                        + " and a.parent_account_id = b.row_id "
                        + " and t.oba_number = :p_target_billto "
                        + " and t.account_version = 'CURRENT' "
                        + " and b.billing_day_of_month != t.billing_day_of_month";
                    log.debug("Executing BDOM validation query: {}", queryStr2);
                    log.debug("Query parameters - p_oba_list: {}, p_target_billto: {}", 
                             shipto.getObaNumbers(), shipto.getTargetBillTo());
                    
                    Query query2 = em.createNativeQuery(queryStr2);
                    query2.setParameter("p_oba_list", shipto.getObaNumbers());
                    query2.setParameter("p_target_billto", shipto.getTargetBillTo());
                    List<?> differentBdomShiptos = query2.getResultList();
                    log.info("Found {} shiptos with different BDOM", differentBdomShiptos.size());
                    
                    differentBdomShiptos.forEach(acc -> {
                        String obaNumber = (String)acc;
                        log.warn("BDOM mismatch for shipto: {}", obaNumber);
                        ErrorDetail error = new ErrorDetail("EFX_SHIPTO_MOVING_TO_DIFFERENT_BDOM", 
                            obaNumber + " cannot be moved to a bill to with different BDOM");
                        retVal.add(error);
                    });
                }
            });
        }

        // shipto move to more than one billto
        Set<String> uniqueShiptos = new HashSet<String>();
        List<String> duplicateShiptos = sourceShiptos.stream()
            .filter(e -> !uniqueShiptos.add(e))
            .collect(Collectors.toList());
        
        if (!duplicateShiptos.isEmpty()) {
            log.warn("Found shiptos being moved to multiple billtos: {}", duplicateShiptos);
            duplicateShiptos.forEach(e -> {
                ErrorDetail error = new ErrorDetail("EFX_SHIPTO_MOVED_MORE_THAN_ONE_BILLTO", 
                    e + " cannot be moved to more than one Billto");
                retVal.add(error);
            });
        }

        // shipto and billto can't be moved together
        if (!sourceShiptos.isEmpty() && !sourceBillTos.isEmpty()) {
            String queryStr3 = "Select a.oba_number from c2o_account a, c2o_account b "
                + " where a.account_version = 'CURRENT' "
                + " and a.oba_number in :p_oba_list "
                + " and a.parent_account_id = b.row_id "
                + " and exists (select 1 from c2o_account bb "
                + "             where bb.account_version = 'CURRENT' "
                + "             and bb.oba_number = b.oba_number "
                + "             and bb.oba_number in :p_billto_list)";
            log.debug("Executing shipto-billto validation query: {}", queryStr3);
            log.debug("Query parameters - p_oba_list: {}, p_billto_list: {}", sourceShiptos, sourceBillTos);
            
            Query query3 = em.createNativeQuery(queryStr3);
            query3.setParameter("p_oba_list", sourceShiptos);
            query3.setParameter("p_billto_list", sourceBillTos);
            List<?> invalidMoves = query3.getResultList();
            log.info("Found {} invalid shipto-billto combinations", invalidMoves.size());
            
            invalidMoves.forEach(acc -> {
                String obaNumber = (String)acc;
                log.warn("Invalid shipto-billto combination found for: {}", obaNumber);
                ErrorDetail error = new ErrorDetail("EFX_SHIPTO_BILLTO_MOVED_TOGETHER", 
                    obaNumber + " cannot be moved with its Billto");
                retVal.add(error);
            });
        }

        log.info("Validation completed with {} errors", retVal.size());
        return retVal;
    }
}
