package com.equifax.c2o.api.ruleEngine.businessRule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;
import com.equifax.c2o.api.ruleEngine.model.EntityType;
import com.equifax.c2o.api.ruleEngine.model.moveaccount.types.MoveAccountRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MoveAccountGroupValidate extends BusinessRule {

    @PersistenceContext
    private EntityManager em;

    @Override
    public String getRuleName() {
        return "MOVE_ACCOUNT_GROUP_VALIDATE";
    }

    @Override
    public List<ErrorDetail> validate(Object inputData) throws Exception {
        log.info("Starting group validation for account move request");
        boolean valid = true;
        List<ErrorDetail> retVal = new ArrayList<ErrorDetail>();

        ObjectMapper mapper = new ObjectMapper();
        MoveAccountRequest requestInput = mapper.readValue((String) inputData, MoveAccountRequest.class);
        log.info("Received request: sourceContractId={}, targetContractId={}",
                requestInput.getSourceContractId(), requestInput.getTargetContractId());

        BigDecimal sourceContractId = requestInput.getSourceContractId();
        if (sourceContractId == null) {
            log.error("Source contract ID is null");
            retVal.add(new ErrorDetail(
                "EFX_C2O_ERR_INVALID_CONTRACT", 
                "Source contract ID is required", 
                EntityType.TRG_CONTRACT_ID.name() + "[null]"
            ));
            return retVal;
        }
        log.debug("Processing source contract ID: {}", sourceContractId);

        // find DSG and AGG from source contract for all moving accounts
        Set<String> sourceShiptos = new HashSet<>();
        if (requestInput.getShipTos() != null && !requestInput.getShipTos().isEmpty()) {
            log.debug("Processing {} ShipTo entries from request", requestInput.getShipTos().size());
            requestInput.getShipTos().forEach(shipto -> {
                if (shipto.getObaNumbers() != null) {
                    Set<String> validObas = shipto.getObaNumbers().stream()
                            .filter(oba -> oba != null && !oba.trim().isEmpty())
                            .collect(Collectors.toSet());
                    log.debug("Adding {} valid OBA numbers from ShipTo", validObas.size());
                    sourceShiptos.addAll(validObas);
                }
            });
        } else {
            log.debug("No ShipTo entries found in request");
        }

        Set<String> sourceBillTos = new HashSet<>();
        if (requestInput.getBillTos() != null && !requestInput.getBillTos().isEmpty()) {
            log.debug("Processing {} BillTo entries from request", requestInput.getBillTos().size());
            sourceBillTos.addAll(requestInput.getBillTos().stream()
                    .filter(billTo -> billTo != null && !billTo.trim().isEmpty())
                    .collect(Collectors.toSet()));
            log.debug("Added {} valid BillTo entries", sourceBillTos.size());
        } else {
            log.debug("No BillTo entries found in request");
        }

        if (sourceShiptos.isEmpty() && sourceBillTos.isEmpty()) {
            log.warn("No valid accounts found in request");
            retVal.add(new ErrorDetail(
                "EFX_C2O_ERR_NO_ACCOUNTS", 
                "No valid accounts provided in request", 
                null
            ));
            return retVal;
        }

        // find direct discount groups
        log.info("Finding direct discount groups for contract {} with {} ShipTos and {} BillTos",
                sourceContractId, sourceShiptos.size(), sourceBillTos.size());
        List<String> directGroupsList = findDiscountGroupsInMoveRequest(sourceContractId,
                new ArrayList<>(sourceShiptos), new ArrayList<>(sourceBillTos));

        if (directGroupsList == null || directGroupsList.isEmpty()) {
            log.info("No direct discount groups found, validation complete");
            return retVal;
        }
        log.info("Found {} direct discount groups", directGroupsList.size());

        // find indirect groups
        log.info("Finding indirect discount groups");
        Set<String> indirectGroupsSet = new HashSet<>();
        for (String groupName : directGroupsList) {
            if (groupName != null && !groupName.trim().isEmpty()) {
                log.debug("Processing indirect groups for direct group: {}", groupName);
                findIndirectDiscountGroups(groupName.trim(), sourceContractId, indirectGroupsSet, new HashSet<>(directGroupsList));
            }
        }
        log.info("Found {} indirect discount groups", indirectGroupsSet.size());

        // find all shiptos of direct and indirect groups
        Set<String> allGroups = new HashSet<>(directGroupsList);
        allGroups.addAll(indirectGroupsSet);
        log.info("Total groups to validate: {}", allGroups.size());

        String queryStr = "select a.oba_number from c2o_account a, c2o_contract_entitlement ce\r\n"
                + "where ce.account_id = a.row_id and ce.contract_id = :p_contract_id\r\n"
                + "and ( ce.sharing_group_name in :p_group_list or ce.aggregator_name in :p_group_list)\r\n"
                + "union\r\n"
                + "select a.oba_number from c2o_account a, c2o_contract_entitlement ce,c2o_contract_charge_offers co\r\n"
                + "where ce.account_id = a.row_id and ce.contract_id = :p_contract_id and ce.contract_id = co.contract_id\r\n"
                + "and ce.line_charge_offer_id = co.id and co.charge_offer_id like '%_2'\r\n"
                + "and co.subscription_link in :p_group_list";
        log.debug("Executing query to find all ShipTos in groups");
        Query query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", sourceContractId);
        query.setParameter("p_group_list", new ArrayList<>(allGroups));
        List<Object> res = query.getResultList();
        Set<String> allShiptos = new HashSet<>();
        if (res != null && res.size() > 0) {
            allShiptos = res.stream()
                    .filter(e -> e != null)
                    .map(e -> (String) e)
                    .collect(Collectors.toSet());
            log.debug("Found {} ShipTos in all groups", allShiptos.size());
        } else {
            log.debug("No ShipTos found in groups");
        }

        queryStr = "select a.oba_number from c2o_account a, c2o_account b\r\n"
                + "where a.parent_account_id =  b.row_id\r\n"
                + "and b.oba_number in :p_billto_list\r\n";
        log.debug("Finding ShipTos under BillTos");
        query = em.createNativeQuery(queryStr);

        query.setParameter("p_billto_list", new ArrayList<>(sourceBillTos));
        res = query.getResultList();
        Set<String> allBillShiptos = new HashSet<>();
        if (res != null && res.size() > 0) {
            allBillShiptos = res.stream()
                    .filter(e -> e != null)
                    .map(e -> (String) e)
                    .collect(Collectors.toSet());
            log.debug("Found {} ShipTos under BillTos", allBillShiptos.size());
        } else {
            log.debug("No ShipTos found under BillTos");
        }

        sourceShiptos.addAll(allBillShiptos);
        log.debug("Total source ShipTos after adding BillTo children: {}", sourceShiptos.size());

        Set<String> missingShiptos = new HashSet<>(allShiptos);
        missingShiptos.removeAll(sourceShiptos);

        if (!missingShiptos.isEmpty()) {
            valid = false;
            log.error("Found {} missing ShipTos from discount groups", missingShiptos.size());
            log.error("Missing ShipTos: {}", String.join(",", missingShiptos));
            missingShiptos.forEach(shipto -> {
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_MISSING_SHIPTOS_DSG",
                    "ShipTo is part of discount groups " + String.join(",", allGroups) + " and must be included",
                    EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + shipto + "]"
                ));
            });
        } else {
            log.info("All required ShipTos are included in the request");
        }

        return retVal;
    }

    private List<String> findDiscountGroupsInMoveRequest(BigDecimal contractId, List<String> shiptoList, List<String> billtoList) {
        log.debug("Finding discount groups for contract {} with {} ShipTos and {} BillTos", contractId, shiptoList.size(),
                billtoList.size());
        List<String> retVal = new ArrayList<>();
        String queryStr = "select distinct ce.sharing_group_name\r\n"
                + "from c2o_contract_entitlement ce, c2o_account a, c2o_account b \r\n"
                + "where ce.contract_id = :p_contract_id\r\n"
                + "and ce.account_id = a.row_id and a.parent_account_id = b.row_id\r\n"
                + "and (a.oba_number in :p_ship_oba_list or a.parent_account_id in :p_bill_oba_list) and ce.sharing_group_name is not null \r\n"
                + "union\r\n"
                + "select distinct ce.aggregator_name\r\n"
                + "from c2o_contract_entitlement ce, c2o_account a, c2o_account b \r\n"
                + "where ce.contract_id = :p_contract_id\r\n"
                + "and ce.account_id = a.row_id and a.parent_account_id = b.row_id\r\n"
                + "and (a.oba_number in :p_ship_oba_list or a.parent_account_id in :p_bill_oba_list) and ce.aggregator_name is not null \r\n"
                + "union\r\n"
                + "select distinct co.subscription_link\r\n"
                + "from c2o_contract_entitlement ce, c2o_account a, c2o_account b, c2o_contract_charge_offers co \r\n"
                + "where ce.contract_id = :p_contract_id and ce.contract_id = co.contract_id and ce.line_charge_offer_id = co.id\r\n"
                + "and ce.account_id = a.row_id and a.parent_account_id = b.row_id and co.charge_offer_id like '%_2'\r\n"
                + "and co.subscription_link is not null\r\n"
                + "and (a.oba_number in :p_ship_oba_list or a.parent_account_id in :p_bill_oba_list)";

        Query query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("p_ship_oba_list", shiptoList);
        query.setParameter("p_bill_oba_list", billtoList);
        retVal = query.getResultList();

        log.debug("Found {} discount groups", retVal.size());
        if (!retVal.isEmpty()) {
            log.debug("Discount groups: {}", String.join(", ", retVal));
        }

        return retVal;
    }

    private void findIndirectDiscountGroups(String groupName, BigDecimal contractId, Set<String> indirectGroupsSet,
            Set<String> directGroupsSet) {
        log.debug("Finding indirect discount groups for group: {} in contract {}", groupName, contractId);

        // find CMT linked AGGRs
        log.debug("Checking for CMT linked aggregators");
        String queryStr = "select co.aggregator_link from c2o_contract_charge_offers co, c2o_contract_line_items cli\r\n"
                + "where cli.contract_id = :p_contract_id and cli.sharing_group_name = :group_name\r\n"
                + "and co.contract_item_line_id = cli.contract_item_line_id and co.primary_co = 'Y' and co.aggregator_link is not null";

        Query query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("group_name", groupName);
        List<Object> res = query.getResultList();
        if (res != null && res.size() > 0) {
            String grName = (String) res.get(0);
            log.debug("Found CMT linked aggregator: {}", grName);
            if (!indirectGroupsSet.contains(grName) && !directGroupsSet.contains(grName)) {
                log.debug("Adding new indirect group: {}", grName);
                indirectGroupsSet.add(grName);
                findIndirectDiscountGroups(grName, contractId, indirectGroupsSet, directGroupsSet);
            } else {
                log.debug("Group {} already exists in direct or indirect list", grName);
            }
        } else {
            log.debug("No CMT linked aggregators found");
        }

        // find AGG linked to CMT DSG
        log.debug("Checking for aggregators linked to CMT DSG");
        queryStr = "select cli.sharing_group_name from c2o_contract_charge_offers co, c2o_contract_line_items cli\r\n"
                + "where cli.contract_id = :p_contract_id and co.contract_item_line_id = cli.contract_item_line_id \r\n"
                + "and co.primary_co = 'Y' and co.charge_offer_id like '%_5' \r\n"
                + "and :aggregator_name in (SELECT regexp_substr(co.Aggregator_link,'[^,]+', 1, LEVEL) items       \r\n"
                + "                            FROM DUAL       \r\n"
                + "                        CONNECT BY REGEXP_SUBSTR(co.Aggregator_link, '[^,]+', 1, LEVEL) IS NOT NULL       \r\n"
                + "                    )";
        query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("aggregator_name", groupName);
        res = query.getResultList();
        if (res != null && res.size() > 0) {
            String grName = (String) res.get(0);
            log.debug("Found aggregator linked to CMT DSG: {}", grName);
            if (!indirectGroupsSet.contains(grName) && !directGroupsSet.contains(grName)) {
                log.debug("Adding new indirect group: {}", grName);
                indirectGroupsSet.add(grName);
                findIndirectDiscountGroups(grName, contractId, indirectGroupsSet, directGroupsSet);
            } else {
                log.debug("Group {} already exists in direct or indirect list", grName);
            }
        } else {
            log.debug("No aggregators linked to CMT DSG found");
        }

        // find discount groups linked to members
        log.debug("Finding members for group: {}", groupName);
        queryStr = "select a.oba_number from c2o_account a, c2o_contract_entitlement ce\r\n"
                + "where ce.account_id = a.row_id and ce.contract_id = :p_contract_id\r\n"
                + "and (ce.sharing_group_name=:p_group_name or ce.aggregator_name = :p_group_name)\r\n"
                + "union\r\n"
                + "select a.oba_number from c2o_account a, c2o_contract_charge_offers co, c2o_contract_entitlement ce\r\n"
                + "where ce.account_id = a.row_id and ce.contract_id = :p_contract_id and ce.line_charge_offer_id = co.id\r\n"
                + "and ce.contract_id = co.contract_id and co.charge_offer_id like '%_2' and co.subscription_link = :p_group_name";
        query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("p_group_name", groupName);
        res = query.getResultList();
        Set<String> members = new HashSet<>();
        if (res != null && res.size() > 0) {
            members = res.stream()
                    .filter(e -> e != null)
                    .map(e -> (String) e)
                    .collect(Collectors.toSet());
            log.debug("Found {} members for group {}", members.size(), groupName);
        } else {
            log.debug("No members found for group {}", groupName);
        }

        if (members.size() > 0) {
            log.debug("Checking for additional discount groups linked to {} members", members.size());
            queryStr = "select distinct ce.sharing_group_name\r\n"
                    + " from c2o_contract_entitlement ce, c2o_account a\r\n"
                    + " where ce.contract_id = :p_contract_id\r\n"
                    + " and ce.account_id = a.row_id\r\n"
                    + "                    and a.oba_number in :p_ship_oba_list\r\n"
                    + "                    and ce.sharing_group_name != :p_group_name and ce.sharing_group_name is not null \r\n"
                    + " union\r\n"
                    + " select distinct ce.aggregator_name\r\n"
                    + " from c2o_contract_entitlement ce, c2o_account a\r\n"
                    + " where ce.contract_id = :p_contract_id\r\n"
                    + " and ce.account_id = a.row_id \r\n"
                    + "                    and a.oba_number in :p_ship_oba_list \r\n"
                    + "                    and ce.aggregator_name != :p_group_name and ce.aggregator_name is not null \r\n"
                    + " union\r\n"
                    + " select distinct co.subscription_link\r\n"
                    + " from c2o_contract_entitlement ce, c2o_account a, c2o_contract_charge_offers co \r\n"
                    + " where ce.contract_id = :p_contract_id and ce.contract_id = co.contract_id\r\n"
                    + "                    and ce.line_charge_offer_id = co.id and ce.account_id = a.row_id  \r\n"
                    + "                    and co.charge_offer_id like '%_2' and co.subscription_link is not null\r\n"
                    + " and a.oba_number in :p_ship_oba_list and co.subscription_link != :p_group_name";
            query = em.createNativeQuery(queryStr);
            query.setParameter("p_contract_id", contractId);
            query.setParameter("p_group_name", groupName);
            query.setParameter("p_ship_oba_list", members);
            res = query.getResultList();
            if (res != null && res.size() > 0) {
                String grName = (String) res.get(0);
                log.debug("Found additional group linked to members: {}", grName);
                if (!indirectGroupsSet.contains(grName) && !directGroupsSet.contains(grName)) {
                    log.debug("Adding new indirect group: {}", grName);
                    indirectGroupsSet.add(grName);
                    findIndirectDiscountGroups(grName, contractId, indirectGroupsSet, directGroupsSet);
                } else {
                    log.debug("Group {} already exists in direct or indirect list", grName);
                }
            } else {
                log.debug("No additional groups found linked to members");
            }
        }
    }
}