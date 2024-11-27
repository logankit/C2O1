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

public class MoveAccountValidate extends BusinessRule{

@PersistenceContext
private EntityManager em;

@Override
public String getRuleName() {
return "MOVE_ACCOUNT_VALIDATE";
}

@Override
public List<ErrorDetail> validate(Object inputData) throws Exception {
ObjectMapper mapper = new ObjectMapper();
MoveAccountRequest requestInput = mapper.readValue((String)inputData, MoveAccountRequest.class);
List<ErrorDetail> retVal = new ArrayList<ErrorDetail>();

List<String> sourceShiptos = new ArrayList<String>();
List<String> targetBillTos = new ArrayList<String>();
if(!requestInput.getShipTos().isEmpty()) {
requestInput.getShipTos().forEach(shipto ->{
sourceShiptos.addAll(shipto.getObaNumbers());
targetBillTos.add(shipto.getTargetBillTo());
});
}
List<String> sourceBillTos = new ArrayList<String>();
if(!requestInput.getBillTos().isEmpty())
sourceBillTos.addAll(requestInput.getBillTos());

//validate accounts are active and correct account type
//validate source shiptos
String queryStr = "Select a.oba_number from c2o_account a where a.account_version = 'CURRENT' "
+ " and (a.account_status != '27' or a.account_type != 'SHIP-TO')"
+ " and a.oba_number in :p_oba_list ";
Query query = em.createNativeQuery(queryStr);
query.setParameter("p_oba_list", sourceShiptos);
query.getResultList().forEach(acc -> {
ErrorDetail error = new ErrorDetail("EFX_NOT_AN_ACTIVE_SHIPTO", (String)acc + " is not an active Shipto");
retVal.add(error);
});

//moving to a different BDOM
if(!requestInput.getShipTos().isEmpty()) {
requestInput.getShipTos().forEach(shipto ->{
String queryStr2 = "Select a.oba_number from c2o_account a, c2o_account b, c2o_account bb"
+ " where a.account_version = 'CURRENT' and a.oba_number in :p_oba_list "
+ " and a.parent_account_id = b.row_id and bb.account_version = 'CURRENT' "
+ " and b.billing_day_of_month != bb.billing_day_of_month";
Query query2 = em.createNativeQuery(queryStr2);
query2.setParameter("p_oba_list", sourceShiptos);
query2.getResultList().forEach(acc -> {
ErrorDetail error = new ErrorDetail("EFX_SHIPTO_MOVING_TO_DIFFERENT_BDOM", (String)acc + " cannot be moved to a bill to with different BDOM");
retVal.add(error);
});
});

//shipto move to more than one billto
Set<String> uniqueShiptos = new HashSet<String>();
sourceShiptos.stream().filter(e -> !uniqueShiptos.add(e))
.forEach(e-> {
ErrorDetail error = new ErrorDetail("EFX_SHIPTO_MOVED_MORE_THAN_ONE_BILLTO", e + " cannot be moved to more than one Billto");
retVal.add(error);
});

// shipto and billto can't be moved together
String queryStr3 = "Select a.oba_number from c2o_account a, c2o_account b where a.account_version = 'CURRENT' "
+ " and a.oba_number in :p_oba_list and a.parent_account_id = b.row_id and "
+ " exists (select 1 from c2o_account bb where bb.account_version = 'CURRENT' and bb.oba_number = b.oba_number and  bb.oba_number in :p_billto_list)";
Query query3 = em.createNativeQuery(queryStr3);
query3.setParameter("p_oba_list", sourceShiptos);
query3.setParameter("p_billto_list", sourceBillTos);
query3.getResultList().forEach(acc -> {
ErrorDetail error = new ErrorDetail("EFX_SHIPTO_BILLTO_CANT_MOVE_TOGETHER", "Shipto "+ (String)acc + " and its billto cannot be moved together");
retVal.add(error);
});

// source billtos active
String queryStr4 = "Select a.oba_number from c2o_account a where a.account_version = 'CURRENT' "
+ " and (a.account_status != '27' or a.account_type != 'BILL-TO')"
+ " and a.oba_number in :p_oba_list ";
Query query4 = em.createNativeQuery(queryStr4);
query4.setParameter("p_oba_list", sourceBillTos);
query4.getResultList().forEach(acc -> {
ErrorDetail error = new ErrorDetail("EFX_NOT_AN_ACTIVE_BILL_TO", (String)acc + " is not an active Billto");
retVal.add(error);
});

// target billto not active
String queryStr5 = "Select a.oba_number from c2o_account a where a.account_version = 'CURRENT' "
+ " and (a.account_status != '27' or a.account_type != 'BILL-TO')"
+ " and a.oba_number in :p_oba_list ";
Query query5 = em.createNativeQuery(queryStr5);
query5.setParameter("p_oba_list", targetBillTos);
query5.getResultList().forEach(acc -> {
ErrorDetail error = new ErrorDetail("EFX_NOT_AN_ACTIVE_BILL_TO", (String)acc + " is not an active Billto");
retVal.add(error);
});

return retVal;
}



}
