package com.equifax.c2o.api.RequestDetails.repository;

import com.equifax.c2o.api.RequestDetails.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
@Query("SELECT r FROM Request r WHERE " +
           "(:sourceSystemId IS NULL OR r.sourceSystemId = :sourceSystemId) AND " +
           "(:fromDate IS NULL OR r.createdDate >= :fromDate) AND " +
           "(:toDate IS NULL OR r.createdDate <= :toDate) AND " +
           "(:businessUnit IS NULL OR r.businessUnit IN :businessUnit) AND " +
           "(:contractId IS NULL OR r.contractId = :contractId) AND " +
           "(:orderId IS NULL OR r.orderId = :orderId) AND " +
           "(:requestStatus IS NULL OR r.requestStatus = :requestStatus)")
    List<Request> findRequests(
            @Param("sourceSystemId") String sourceSystemId,
            @Param("fromDate") Timestamp fromDate,
            @Param("toDate") Timestamp toDate,
            @Param("businessUnit") List<String> businessUnit,
            @Param("contractId") Long contractId,
            @Param("orderId") Long orderId,
            @Param("requestStatus") Integer requestStatus);

    Request findByCorrelationId(String correlationId);
}
