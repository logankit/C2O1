package com.equifax.c2o.api.RequestDetails.repository;

import com.equifax.c2o.api.RequestDetails.model.Request;

import java.sql.Timestamp;
import java.util.List;

public interface CustomRequestRepository {
    List<Request> findRequests(String sourceSystemId, Timestamp fromDate, Timestamp toDate, List<String> businessUnit,
                               Long contractId, Long orderId, Integer requestStatus);
}
