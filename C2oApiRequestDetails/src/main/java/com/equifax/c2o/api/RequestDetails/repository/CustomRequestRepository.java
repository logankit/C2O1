package com.equifax.c2o.api.RequestDetails.repository;

import com.equifax.c2o.api.RequestDetails.model.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;

public interface CustomRequestRepository {
    Page<Request> findRequests(String sourceSystem, Timestamp fromDate, Timestamp toDate, String businessUnit, Long contractId, Long orderId, Integer requestStatus, Pageable pageable);
}
