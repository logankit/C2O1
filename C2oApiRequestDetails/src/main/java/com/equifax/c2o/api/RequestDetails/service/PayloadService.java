package com.equifax.c2o.api.RequestDetails.service;

import com.equifax.c2o.api.RequestDetails.dto.PayloadResponseDTO;
import com.equifax.c2o.api.RequestDetails.exception.CustomException;
import com.equifax.c2o.api.RequestDetails.model.Request;
import com.equifax.c2o.api.RequestDetails.repository.RequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PayloadService {

    @Autowired
    private RequestRepository requestRepository;

    public PayloadResponseDTO getPayload(String correlationId, String reqType) {
        Request request = requestRepository.findByCorrelationId(correlationId);
        if (request == null) {
            throw new CustomException("EFX_COR_ID_NOT_FOUND", "No request payload found with mentioned correlation id: " + correlationId);
        }

        PayloadResponseDTO payloadResponseDTO = new PayloadResponseDTO();
        if ("RequestPayload".equalsIgnoreCase(reqType)) {
            payloadResponseDTO.setRequestPayload(request.getRequestPayload2());
        } else if ("ResponsePayload".equalsIgnoreCase(reqType)) {
            payloadResponseDTO.setResponsePayload(request.getResponsePayload());
        } else {
            throw new CustomException("EFX_INVALID_REQ_TYPE", "Invalid request type: " + reqType);
        }

        return payloadResponseDTO;
    }
}
