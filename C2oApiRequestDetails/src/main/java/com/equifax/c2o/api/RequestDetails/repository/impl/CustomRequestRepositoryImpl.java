package com.equifax.c2o.api.RequestDetails.repository.impl;

import com.equifax.c2o.api.RequestDetails.model.Request;
import com.equifax.c2o.api.RequestDetails.repository.CustomRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class CustomRequestRepositoryImpl implements CustomRequestRepository {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Request> findRequests(String sourceSystemId, Timestamp fromDate, Timestamp toDate, List<String> businessUnit,
                                      Long contractId, Long orderId, Integer requestStatus) {
        String jpql = "SELECT r FROM Request r WHERE " +
                "(:sourceSystemId IS NULL OR r.sourceSystemId = :sourceSystemId) AND " +
                "(:fromDate IS NULL OR r.createdDate >= :fromDate) AND " +
                "(:toDate IS NULL OR r.createdDate <= :toDate) AND " +
                "(:businessUnit IS NULL OR r.businessUnit IN :businessUnit) AND " +
                "(:contractId IS NULL OR r.contractId = :contractId) AND " +
                "(:orderId IS NULL OR r.orderId = :orderId) AND " +
                "(:requestStatus IS NULL OR r.requestStatus = :requestStatus)";
        
        Query query = entityManager.createQuery(jpql, Request.class);
        query.setParameter("sourceSystemId", sourceSystemId);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("businessUnit", businessUnit);
        query.setParameter("contractId", contractId);
        query.setParameter("orderId", orderId);
        query.setParameter("requestStatus", requestStatus);
        
        // Log the query and parameters
        System.out.println("Executing query: " + jpql);
        System.out.println("Parameters:");
        System.out.println("sourceSystemId = " + sourceSystemId);
        System.out.println("fromDate = " + fromDate);
        System.out.println("toDate = " + toDate);
        System.out.println("businessUnit = " + businessUnit);
        System.out.println("contractId = " + contractId);
        System.out.println("orderId = " + orderId);
        System.out.println("requestStatus = " + requestStatus);
        
        return query.getResultList();
    }
}
