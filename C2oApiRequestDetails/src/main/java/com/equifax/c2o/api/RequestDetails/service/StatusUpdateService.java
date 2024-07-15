package com.equifax.c2o.api.RequestDetails.service;

import com.equifax.c2o.api.RequestDetails.dto.StatusUpdateRequest;
import com.equifax.c2o.api.RequestDetails.exception.CustomException;
import com.equifax.c2o.api.RequestDetails.model.Request;
import com.equifax.c2o.api.RequestDetails.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatusUpdateService {

    @Autowired
    private RequestRepository requestRepository;

    public void updateStatus(StatusUpdateRequest statusUpdateRequest) {
        Request request = requestRepository.findByCorrelationId(statusUpdateRequest.getRootCorrelationId());
        if (request == null) {
            throw new CustomException("EFX_COR_ID_NOT_FOUND", "No request found with the mentioned correlation id: " + statusUpdateRequest.getRootCorrelationId());
        }

        request.setRequestStatus(318); // Assuming 318 is the new status to be set
        requestRepository.save(request);
    }
}
