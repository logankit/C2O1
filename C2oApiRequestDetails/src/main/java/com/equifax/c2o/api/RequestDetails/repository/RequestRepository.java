package com.equifax.c2o.api.RequestDetails.repository;

import com.equifax.c2o.api.RequestDetails.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long>, CustomRequestRepository {
    Request findByCorrelationId(String correlationId);
}
