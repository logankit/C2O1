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
        List<ErrorDetail> retVal = new ArrayList<>();

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

        // find DSG and AGG from source contract for all moving accounts
        Set<String> sourceShiptos = new HashSet<>();
        if (requestInput.getShipTos() != null && !requestInput.getShipTos().isEmpty()) {
            requestInput.getShipTos().forEach(shipto -> {
                if (shipto.getObaNumbers() != null) {
                    sourceShiptos.addAll(shipto.getObaNumbers().stream()
                        .filter(oba -> oba != null && !oba.trim().isEmpty())
                        .collect(Collectors.toSet()));
                }
            });
        }

        Set<String> sourceBillTos = new HashSet<>();
        if (requestInput.getBillTos() != null && !requestInput.getBillTos().isEmpty()) {
            sourceBillTos.addAll(requestInput.getBillTos().stream()
                .filter(billTo -> billTo != null && !billTo.trim().isEmpty())
                .collect(Collectors.toSet()));
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
        List<String> directGroupsList = findDiscountGroupsInMoveRequest(sourceContractId,
                new ArrayList<>(sourceShiptos), new ArrayList<>(sourceBillTos));

        if (directGroupsList == null || directGroupsList.isEmpty()) {
            log.info("No direct discount groups found, validation complete");
            return retVal;
        }

        // find indirect groups
        Set<String> indirectGroupsSet = new HashSet<>();
        for (String groupName : directGroupsList) {
            if (groupName != null && !groupName.trim().isEmpty()) {
                findIndirectDiscountGroups(groupName.trim(), sourceContractId, indirectGroupsSet, new HashSet<>(directGroupsList));
            }
        }

        // find all shiptos of direct and indirect groups
        Set<String> allGroups = new HashSet<>(directGroupsList);
        allGroups.addAll(indirectGroupsSet);

        String queryStr = "select a.oba_number from c2o_account a, c2o_contract_entitlement ce\n" +
                "where ce.account_id = a.row_id and ce.contract_id = :p_contract_id\n" +
                "and ( ce.sharing_group_name in :p_group_list or ce.aggregator_name in :p_group_list)\n" +
                "union\n" +
                "select a.oba_number from c2o_account a, c2o_contract_entitlement ce,c2o_contract_charge_offers co\n" +
                "where ce.account_id = a.row_id and ce.contract_id = :p_contract_id and ce.contract_id = co.contract_id\n" +
                "and ce.line_charge_offer_id = co.id and co.charge_offer_id like '%_2'\n" +
                "and co.subscription_link in :p_group_list";

        Query query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", sourceContractId);
        query.setParameter("p_group_list", new ArrayList<>(allGroups));
        List<String> groupShiptos = query.getResultList();

        // Check if any group members are not included in the move request
        Set<String> unmovingShiptos = new HashSet<>(groupShiptos);
        unmovingShiptos.removeAll(sourceShiptos);

        if (!unmovingShiptos.isEmpty()) {
            log.error("Found {} missing ShipTos from discount groups", unmovingShiptos.size());
            unmovingShiptos.forEach(shipto -> {
                retVal.add(new ErrorDetail(
                    "EFX_C2O_ERR_GROUP_MEMBER_NOT_INCLUDED",
                    String.format("ShipTo is part of discount groups [%s] and must be included", 
                        String.join(", ", allGroups)),
                    EntityType.SHIP_TO_OBA_NUMBER.name() + "[" + shipto + "]"
                ));
            });
        }

        return retVal;
    }

    private List<String> findDiscountGroupsInMoveRequest(BigDecimal contractId, List<String> shiptoList, List<String> billtoList) {
        log.debug("Finding discount groups for contract {} with {} ShipTos and {} BillTos", contractId, shiptoList.size(),
                billtoList.size());
        List<String> retVal = new ArrayList<>();
        String queryStr = "select distinct ce.sharing_group_name\r\n"
                + "from c2o_contract_entitlement ce, c2o_account a \r\n"
                + "where ce.contract_id = :p_contract_id\r\n"
                + "and ce.account_id = a.row_id\r\n"
                + "and a.oba_number in :p_ship_oba_list and ce.sharing_group_name is not null \r\n"
                + "union\r\n"
                + "select distinct ce.aggregator_name\r\n"
                + "from c2o_contract_entitlement ce, c2o_account a \r\n"
                + "where ce.contract_id = :p_contract_id\r\n"
                + "and ce.account_id = a.row_id\r\n"
                + "and a.oba_number in :p_ship_oba_list and ce.aggregator_name is not null \r\n"
                + "union\r\n"
                + "select distinct co.subscription_link\r\n"
                + "from c2o_contract_entitlement ce, c2o_account a, c2o_contract_charge_offers co \r\n"
                + "where ce.contract_id = :p_contract_id and ce.contract_id = co.contract_id and ce.line_charge_offer_id = co.id\r\n"
                + "and ce.account_id = a.row_id and co.charge_offer_id like '%_2'\r\n"
                + "and co.subscription_link is not null\r\n"
                + "and a.oba_number in :p_ship_oba_list";

        Query query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("p_ship_oba_list", shiptoList);

        retVal = query.getResultList();

        // If we have bill-to OBA numbers, add them to the search
        if (!billtoList.isEmpty()) {
            String billToQueryStr = "select distinct ce.sharing_group_name\r\n"
                    + "from c2o_contract_entitlement ce, c2o_account a \r\n"
                    + "where ce.contract_id = :p_contract_id\r\n"
                    + "and ce.account_id = a.row_id\r\n"
                    + "and a.oba_number in :p_bill_oba_list and ce.sharing_group_name is not null \r\n"
                    + "union\r\n"
                    + "select distinct ce.aggregator_name\r\n"
                    + "from c2o_contract_entitlement ce, c2o_account a \r\n"
                    + "where ce.contract_id = :p_contract_id\r\n"
                    + "and ce.account_id = a.row_id\r\n"
                    + "and a.oba_number in :p_bill_oba_list and ce.aggregator_name is not null \r\n"
                    + "union\r\n"
                    + "select distinct co.subscription_link\r\n"
                    + "from c2o_contract_entitlement ce, c2o_account a, c2o_contract_charge_offers co \r\n"
                    + "where ce.contract_id = :p_contract_id and ce.contract_id = co.contract_id and ce.line_charge_offer_id = co.id\r\n"
                    + "and ce.account_id = a.row_id and co.charge_offer_id like '%_2'\r\n"
                    + "and co.subscription_link is not null\r\n"
                    + "and a.oba_number in :p_bill_oba_list";

            Query billToQuery = em.createNativeQuery(billToQueryStr);
            billToQuery.setParameter("p_contract_id", contractId);
            billToQuery.setParameter("p_bill_oba_list", billtoList);
            
            List<String> billToResults = billToQuery.getResultList();
            retVal.addAll(billToResults);
        }

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
        String queryStr = "SELECT co.aggregator_link " +
            "FROM c2o_contract_charge_offers co, c2o_contract_line_items cli " +
            "WHERE cli.contract_id = :p_contract_id " +
            "AND cli.sharing_group_name = :group_name " +
            "AND co.contract_item_line_id = cli.contract_item_line_id " +
            "AND co.primary_co = 'Y' " +
            "AND co.aggregator_link IS NOT NULL";

        Query query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("group_name", groupName);
        List<?> res = query.getResultList();
        
        if (res != null && !res.isEmpty()) {
            String aggrLink = (String) res.get(0);
            log.debug("Found aggregator link: {}", aggrLink);
            
            // Process each comma-separated aggregator
            for (String aggr : aggrLink.split(",")) {
                String trimmedAggr = aggr.trim();
                if (!trimmedAggr.isEmpty() && !indirectGroupsSet.contains(trimmedAggr) && !directGroupsSet.contains(trimmedAggr)) {
                    log.debug("Adding new indirect group from aggregator link: {}", trimmedAggr);
                    indirectGroupsSet.add(trimmedAggr);
                    findIndirectDiscountGroups(trimmedAggr, contractId, indirectGroupsSet, directGroupsSet);
                }
            }
        }

        // find AGG linked to CMT DSG
        queryStr = "SELECT cli.sharing_group_name " +
            "FROM c2o_contract_charge_offers co " +
            "JOIN c2o_contract_line_items cli ON co.contract_item_line_id = cli.contract_item_line_id " +
            "WHERE cli.contract_id = :p_contract_id " +
            "AND co.primary_co = 'Y' " +
            "AND co.charge_offer_id LIKE '%_5' " +
            "AND co.aggregator_link LIKE :aggregator_pattern";

        query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("aggregator_pattern", "%" + groupName + "%");
        res = query.getResultList();
        
        if (res != null && !res.isEmpty()) {
            String grName = (String) res.get(0);
            if (!indirectGroupsSet.contains(grName) && !directGroupsSet.contains(grName)) {
                log.debug("Adding new indirect group from CMT DSG: {}", grName);
                indirectGroupsSet.add(grName);
                findIndirectDiscountGroups(grName, contractId, indirectGroupsSet, directGroupsSet);
            }
        }

        // find members and their discount groups
        queryStr = "SELECT DISTINCT m.group_name " +
            "FROM (" +
            "    SELECT a.oba_number AS member, ce.sharing_group_name AS group_name " +
            "    FROM c2o_account a " +
            "    JOIN c2o_contract_entitlement ce ON ce.account_id = a.row_id " +
            "    WHERE ce.contract_id = :p_contract_id " +
            "    AND (ce.sharing_group_name = :p_group_name OR ce.aggregator_name = :p_group_name) " +
            "    AND ce.sharing_group_name IS NOT NULL " +
            "    AND ce.sharing_group_name != :p_group_name " +
            "    UNION " +
            "    SELECT a.oba_number AS member, ce.aggregator_name AS group_name " +
            "    FROM c2o_account a " +
            "    JOIN c2o_contract_entitlement ce ON ce.account_id = a.row_id " +
            "    WHERE ce.contract_id = :p_contract_id " +
            "    AND (ce.sharing_group_name = :p_group_name OR ce.aggregator_name = :p_group_name) " +
            "    AND ce.aggregator_name IS NOT NULL " +
            "    AND ce.aggregator_name != :p_group_name " +
            "    UNION " +
            "    SELECT a.oba_number AS member, co.subscription_link AS group_name " +
            "    FROM c2o_account a " +
            "    JOIN c2o_contract_entitlement ce ON ce.account_id = a.row_id " +
            "    JOIN c2o_contract_charge_offers co ON ce.line_charge_offer_id = co.id " +
            "    WHERE ce.contract_id = :p_contract_id " +
            "    AND ce.contract_id = co.contract_id " +
            "    AND co.charge_offer_id LIKE '%_2' " +
            "    AND co.subscription_link IS NOT NULL " +
            "    AND co.subscription_link != :p_group_name" +
            ") m";

        query = em.createNativeQuery(queryStr);
        query.setParameter("p_contract_id", contractId);
        query.setParameter("p_group_name", groupName);
        res = query.getResultList();
        
        if (res != null && !res.isEmpty()) {
            res.stream()
                .filter(e -> e != null)
                .map(e -> (String) e)
                .filter(grName -> !indirectGroupsSet.contains(grName) && !directGroupsSet.contains(grName))
                .forEach(grName -> {
                    log.debug("Adding new indirect group from member relationships: {}", grName);
                    indirectGroupsSet.add(grName);
                    findIndirectDiscountGroups(grName, contractId, indirectGroupsSet, directGroupsSet);
                });
        }
    }
}