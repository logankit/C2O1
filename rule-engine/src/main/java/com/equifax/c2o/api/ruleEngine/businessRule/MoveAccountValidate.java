package com.equifax.c2o.api.ruleEngine.businessRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import com.equifax.c2o.api.ruleEngine.model.EntityType;
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

import java.math.BigDecimal;
import java.util.HashMap;

@Slf4j
@Component
public class MoveAccountValidate extends BusinessRule {

    @PersistenceContext
    private EntityManager em;

    private static final String ACCOUNT_VERSION_CURRENT = "CURRENT";
    private static final String ACCOUNT_TYPE_SHIPTO = "SHIP-TO";
    private static final String ACCOUNT_TYPE_BILLTO = "BILL-TO";
    private static final String ACTIVE_STATUS = "27";

    @Override
    public String getRuleName() {
        return "MOVE_ACCOUNT_VALIDATE";
    }

    @Override
    public List<ErrorDetail> validate(Object inputData) throws Exception {
        log.info("Starting validation for MOVE_ACCOUNT_VALIDATE");
        ObjectMapper mapper = new ObjectMapper();
        MoveAccountRequest requestInput;
        
        if (inputData instanceof String) {
            requestInput = mapper.readValue((String)inputData, MoveAccountRequest.class);
        } else {
            requestInput = mapper.convertValue(inputData, MoveAccountRequest.class);
        }
        
        log.info("Received request: sourceContractId={}, targetContractId={}", 
                requestInput.getSourceContractId(), requestInput.getTargetContractId());

        List<ErrorDetail> retVal = new ArrayList<ErrorDetail>();
        boolean hasValidationErrors = false;

        // Validate required fields
        if (requestInput.getSourceContractId() == null || requestInput.getTargetContractId() == null) {
            log.error("Source or Target Contract ID is missing");
            String entityValue = "";
            if (requestInput.getSourceContractId() != null) {
                entityValue = requestInput.getSourceContractId().toPlainString();
            } else if (requestInput.getTargetContractId() != null) {
                entityValue = requestInput.getTargetContractId().toPlainString();
            }
            retVal.add(new ErrorDetail(
                "EFX_C2O_ERR_MISSING_CONTRACT_ID", 
                "Source and Target Contract IDs are required", 
                EntityType.TRG_CONTRACT_ID.name() + "[" + entityValue + "]"
            ));
            hasValidationErrors = true;
        }

        // Validate contracts are different when moving bill-to accounts
        if (requestInput.getSourceContractId() != null && requestInput.getTargetContractId() != null &&
            requestInput.getSourceContractId().compareTo(requestInput.getTargetContractId()) == 0 &&
            requestInput.getBillTos() != null && !requestInput.getBillTos().isEmpty()) {
            log.error("Source and Target Contract IDs are the same when moving bill-to accounts: {}", 
                    requestInput.getSourceContractId().toPlainString());
            retVal.add(new ErrorDetail(
                "EFX_C2O_ERR_SAME_CONTRACT_BILLTO", 
                "Source and Target Contract IDs cannot be the same when moving bill-to accounts", 
                EntityType.TRG_CONTRACT_ID.name() + "[" + requestInput.getSourceContractId().toPlainString() + "]"
            ));
            hasValidationErrors = true;
        }

        List<String> sourceShiptos = new ArrayList<String>();
        List<String> targetBillTos = new ArrayList<String>();
        if(requestInput.getShipTos() != null && !requestInput.getShipTos().isEmpty()) {
            requestInput.getShipTos().forEach(shipto -> {
                if (shipto.getObaNumbers() != null) {
                    sourceShiptos.addAll(shipto.getObaNumbers().stream()
                        .filter(oba -> oba != null && !oba.trim().isEmpty())
                        .collect(Collectors.toList()));
                }
                if (shipto.getTargetBillTo() != null && !shipto.getTargetBillTo().trim().isEmpty()) {
                    targetBillTos.add(shipto.getTargetBillTo());
                }
            });
            log.info("Processing shipTos - Source ShipTos: {}, Target BillTos: {}", 
                    sourceShiptos, targetBillTos);
        }

        List<String> sourceBillTos = new ArrayList<String>();
        if(requestInput.getBillTos() != null && !requestInput.getBillTos().isEmpty()) {
            sourceBillTos.addAll(requestInput.getBillTos().stream()
                .filter(billTo -> billTo != null && !billTo.trim().isEmpty())
                .collect(Collectors.toList()));
            log.info("Processing billTos - Source BillTos: {}", sourceBillTos);
        }

        // Skip validations if no accounts to process
        if (sourceShiptos.isEmpty() && sourceBillTos.isEmpty()) {
            log.warn("No accounts to process in the request");
            retVal.add(new ErrorDetail(
                "EFX_C2O_ERR_NO_ACCOUNTS", 
                "No accounts provided for processing", 
                null
            ));
            hasValidationErrors = true;
        }

        // Validate target billtos are provided if shiptos are present
        if (!sourceShiptos.isEmpty() && targetBillTos.isEmpty()) {
            log.error("Target BillTos are missing for ShipTos");
            sourceShiptos.forEach(shipto -> {
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_MISSING_TARGET_BILLTO",
                    "Target BillTo must be provided for ShipTo",
                    EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + shipto + "]"
                ));
            });
            hasValidationErrors = true;
        }

        // Validate all accounts exist in c2o_account table
        List<String> allAccounts = new ArrayList<>();
        allAccounts.addAll(sourceShiptos);
        allAccounts.addAll(sourceBillTos);
        allAccounts.addAll(targetBillTos);

        if (!allAccounts.isEmpty()) {
            String existenceQuery = "SELECT a.oba_number, a.account_type FROM c2o_account a "
                + "WHERE a.account_version = :version "
                + "AND a.oba_number IN :p_oba_list";
            
            log.debug("Executing account existence query: {}", existenceQuery);
            log.debug("Query parameters - p_oba_list: {}", allAccounts);
            
            Query query = em.createNativeQuery(existenceQuery);
            query.setParameter("version", ACCOUNT_VERSION_CURRENT);
            query.setParameter("p_oba_list", allAccounts);
            
            List<Object[]> existingAccounts = query.getResultList();
            Map<String, String> accountTypes = new HashMap<>();
            existingAccounts.forEach(row -> accountTypes.put((String)row[0], (String)row[1]));
            
            // Find accounts that don't exist
            List<String> nonExistentAccounts = allAccounts.stream()
                .filter(acc -> !accountTypes.containsKey(acc))
                .collect(Collectors.toList());
            
            if (!nonExistentAccounts.isEmpty()) {
                log.warn("Found non-existent accounts: {}", nonExistentAccounts);
                nonExistentAccounts.forEach(acc -> {
                    String entityType = sourceShiptos.contains(acc) ? EntityType.SHIP_TO_OBA_NUMBER.name() :
                                      sourceBillTos.contains(acc) ? EntityType.BILL_TO_OBA_NUMBER.name() :
                                      EntityType.TRG_BILL_TO_OBA_NUMBER.name();
                    retVal.add(new ErrorDetail(
                        "EFX_C2O_ERR_ACCOUNT_NOT_FOUND",
                        "Account not found: " + acc,
                        entityType + "[" + acc + "]"
                    ));
                });
                hasValidationErrors = true;
            }
            
            // Validate account types
            List<String> invalidShipTos = sourceShiptos.stream()
                .filter(acc -> !ACCOUNT_TYPE_SHIPTO.equals(accountTypes.get(acc)))
                .collect(Collectors.toList());
                
            if (!invalidShipTos.isEmpty()) {
                log.warn("Found non-shipto accounts in shipto list: {}", invalidShipTos);
                invalidShipTos.forEach(acc -> {
                    ErrorDetail error = new ErrorDetail("EFX_INVALID_ACCOUNT_TYPE", 
                        "Account " + acc + " is not a ShipTo account",
                        EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + acc + "]");
                    retVal.add(error);
                });
                hasValidationErrors = true;
            }
            
            List<String> invalidBillTos = Stream.concat(sourceBillTos.stream(), targetBillTos.stream())
                .filter(acc -> !ACCOUNT_TYPE_BILLTO.equals(accountTypes.get(acc)))
                .collect(Collectors.toList());
                
            if (!invalidBillTos.isEmpty()) {
                log.warn("Found non-billto accounts in billto list: {}", invalidBillTos);
                invalidBillTos.forEach(acc -> {
                    ErrorDetail error = new ErrorDetail("EFX_INVALID_ACCOUNT_TYPE", 
                        "Account " + acc + " is not a BillTo account",
                        sourceBillTos.contains(acc) ? EntityType.BILL_TO_OBA_NUMBER.name() + "[" + acc + "]" : EntityType.TRG_BILL_TO_OBA_NUMBER.name() + "[" + acc + "]");
                    retVal.add(error);
                });
                hasValidationErrors = true;
            }
            
            log.info("All accounts exist and have correct types");
        }

        // validate source shiptos are active
        if (!sourceShiptos.isEmpty()) {
            String queryStr = "Select a.oba_number from c2o_account a where a.account_version = :version "
                + " and a.account_status = :status "
                + " and a.account_type = :type "
                + " and a.oba_number in :p_oba_list ";
            log.debug("Executing active shiptos query: {}", queryStr);
            log.debug("Query parameters - p_oba_list: {}, status: {}, type: {}", 
                sourceShiptos, ACTIVE_STATUS, ACCOUNT_TYPE_SHIPTO);
            
            Query query = em.createNativeQuery(queryStr);
            query.setParameter("version", ACCOUNT_VERSION_CURRENT);
            query.setParameter("status", ACTIVE_STATUS);
            query.setParameter("type", ACCOUNT_TYPE_SHIPTO);
            query.setParameter("p_oba_list", sourceShiptos);
            
            List<String> activeShiptos = query.getResultList();
            List<String> inactiveShiptos = sourceShiptos.stream()
                .filter(shipto -> !activeShiptos.contains(shipto))
                .collect(Collectors.toList());
            
            log.info("Found {} inactive shiptos", inactiveShiptos.size());
            
            inactiveShiptos.forEach(obaNumber -> {
                log.warn("Inactive shipto found: {}", obaNumber);
                retVal.add(new ErrorDetail(
                    "EFX_NOT_AN_ACTIVE_SHIPTO", 
                    obaNumber + " is not an active Shipto",
                    EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + obaNumber + "]"
                ));
            });
            
            if (!inactiveShiptos.isEmpty()) {
                hasValidationErrors = true;
            }
        }

        // moving to different billing day of the month validate
        if(!sourceShiptos.isEmpty() && !targetBillTos.isEmpty()) {
            String queryStr2 = "SELECT a.oba_number, b.billing_day_of_month as source_bdom, t.billing_day_of_month as target_bdom " +
                "FROM c2o_account a, c2o_account b, c2o_account t " +
                "WHERE a.parent_account_id = b.row_id " +
                "AND t.oba_number = :p_target_billto " +
                "AND a.account_version = :version " +
                "AND b.account_version = :version " +
                "AND t.account_version = :version " +
                "AND a.oba_number IN :p_oba_list " +
                "AND b.billing_day_of_month != t.billing_day_of_month";

            requestInput.getShipTos().forEach(shipto -> {
                if (shipto.getObaNumbers() != null && !shipto.getObaNumbers().isEmpty() 
                    && shipto.getTargetBillTo() != null) {
                    log.info("Validating BDOM - ShipTo(s): {}, Target BillTo: {}", 
                            shipto.getObaNumbers(), shipto.getTargetBillTo());
                    log.debug("BDOM validation query: {}", queryStr2);
                    
                    Query query2 = em.createNativeQuery(queryStr2);
                    query2.setParameter("version", ACCOUNT_VERSION_CURRENT);
                    query2.setParameter("p_oba_list", shipto.getObaNumbers());
                    query2.setParameter("p_target_billto", shipto.getTargetBillTo());
                    
                    List<Object[]> bdomResults = query2.getResultList();
                    log.info("Found {} BDOM mismatches", bdomResults.size());
                    
                    if (!bdomResults.isEmpty()) {
                        bdomResults.forEach(result -> {
                            String shiptoOba = result[0].toString();
                            String sourceBdom = result[1] != null ? result[1].toString() : null;
                            String targetBdom = result[2] != null ? result[2].toString() : null;
                            
                            log.warn("BDOM mismatch detected - ShipTo: {}, Source BDOM: {}, Target BDOM: {}", 
                                    shiptoOba, sourceBdom, targetBdom);
                            retVal.add(new ErrorDetail(
                                "EFX_SHIPTO_MOVING_TO_DIFFERENT_BDOM", 
                                String.format("ShipTo %s cannot be moved from BDOM %s to BDOM %s", 
                                    shiptoOba, sourceBdom, targetBdom),
                                EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + shiptoOba + "]"
                            ));
                        });
                    } else {
                        log.info("No BDOM mismatches found for ShipTo(s): {}", shipto.getObaNumbers());
                    }
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
                retVal.add(new ErrorDetail(
                    "EFX_SHIPTO_MOVED_MORE_THAN_ONE_BILLTO", 
                    e + " cannot be moved to more than one Billto",
                    EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + e + "]"
                ));
            });
            hasValidationErrors = true;
        }

        // shipto and billto can't be moved together
        if (!sourceShiptos.isEmpty() && !sourceBillTos.isEmpty()) {
            String queryStr3 = "SELECT a.oba_number " +
                "FROM c2o_account a, c2o_account b, c2o_account bb " +
                "WHERE a.parent_account_id = b.row_id " +
                "AND bb.account_version = :version " +
                "AND bb.oba_number = b.oba_number " +
                "AND bb.oba_number IN :p_billto_list " +
                "AND a.account_version = :version " +
                "AND a.oba_number IN :p_oba_list";

            log.debug("Executing shipto-billto validation query: {}", queryStr3);
            log.debug("Query parameters - p_oba_list: {}, p_billto_list: {}", sourceShiptos, sourceBillTos);
            
            Query query3 = em.createNativeQuery(queryStr3);
            query3.setParameter("version", ACCOUNT_VERSION_CURRENT);
            query3.setParameter("p_oba_list", sourceShiptos);
            query3.setParameter("p_billto_list", sourceBillTos);
            List<?> invalidMoves = query3.getResultList();
            log.info("Found {} invalid shipto-billto combinations", invalidMoves.size());
            
            invalidMoves.forEach(acc -> {
                String obaNumber = (String)acc;
                log.warn("Invalid shipto-billto combination found for: {}", obaNumber);
                retVal.add(new ErrorDetail(
                    "EFX_SHIPTO_BILLTO_MOVED_TOGETHER", 
                    obaNumber + " cannot be moved with its Billto",
                    EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + obaNumber + "]"
                ));
            });
        }

        if (!hasValidationErrors) {
            log.info("Validation completed successfully");
        } else {
            log.error("Validation failed with {} errors", retVal.size());
        }
        
        return retVal;
    }
}
