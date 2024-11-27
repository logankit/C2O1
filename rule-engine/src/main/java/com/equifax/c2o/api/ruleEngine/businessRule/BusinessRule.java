package com.equifax.c2o.api.ruleEngine.businessRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;



import com.equifax.c2o.api.ruleEngine.model.ErrorDetail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public abstract class BusinessRule {



public abstract List<ErrorDetail> validate(Object inputData) throws Exception;

public abstract String getRuleName();

}