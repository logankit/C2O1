package com.equifax.c2o.api.RequestDetails.repository.impl;

import com.equifax.c2o.api.RequestDetails.model.Request;
import com.equifax.c2o.api.RequestDetails.repository.CustomRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    public Page<Request> findRequests(String sourceSystem, Timestamp fromDate, Timestamp toDate, String businessUnit, Long contractId, Long orderId, Integer requestStatus, Pageable pageable) {
        String jpql = "SELECT r FROM Request r " +
                "WHERE (:sourceSystem IS NULL OR r.sourceSystemId = :sourceSystem) " +
                "AND (:fromDate IS NULL OR r.date >= :fromDate) " +
                "AND (:toDate IS NULL OR r.date <= :toDate) " +
                "AND (:businessUnit IS NULL OR r.businessUnit = :businessUnit) " +
                "AND (:contractId IS NULL OR r.contractId = :contractId) " +
                "AND (:orderId IS NULL OR r.orderId = :orderId) " +
                "AND (:requestStatus IS NULL OR r.requestStatus = :requestStatus)";

        Query query = entityManager.createQuery(jpql);
        
        query.setParameter("sourceSystem", sourceSystem);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        query.setParameter("businessUnit", businessUnit);
        query.setParameter("contractId", contractId);
        query.setParameter("orderId", orderId);
        query.setParameter("requestStatus", requestStatus);

        int totalRows = query.getResultList().size();
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Request> resultList = query.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }
}
