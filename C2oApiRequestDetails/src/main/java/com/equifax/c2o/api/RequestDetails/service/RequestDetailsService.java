package com.equifax.c2o.api.RequestDetails.service;

import com.equifax.c2o.api.RequestDetails.dto.RequestDetailsDTO;
import com.equifax.c2o.api.RequestDetails.dto.RequestDetailsResponseDTO;
import com.equifax.c2o.api.RequestDetails.exception.CustomException;
import com.equifax.c2o.api.RequestDetails.model.Request;
import com.equifax.c2o.api.RequestDetails.repository.CustomRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequestDetailsService {

    @Autowired
    private CustomRequestRepository customRequestRepository;

    public RequestDetailsResponseDTO getRequestDetails(RequestDetailsDTO requestDetailsDTO) {
        validateRequestDetails(requestDetailsDTO);

        Timestamp toDate = requestDetailsDTO.getToDate() != null ? requestDetailsDTO.getToDate() : new Timestamp(System.currentTimeMillis());

        PageRequest pageRequest = PageRequest.of(requestDetailsDTO.getStartIndex(), requestDetailsDTO.getPageLength());

        Page<Request> requestsPage = customRequestRepository.findRequests(
                requestDetailsDTO.getSourceSystem(),
                requestDetailsDTO.getFromDate(),
                toDate,
                requestDetailsDTO.getBusinessUnit(),
                requestDetailsDTO.getContractId() != null ? Long.parseLong(requestDetailsDTO.getContractId()) : null,
                requestDetailsDTO.getOrderId() != null ? Long.parseLong(requestDetailsDTO.getOrderId()) : null,
                requestDetailsDTO.getRequestStatus() != null ? Integer.parseInt(requestDetailsDTO.getRequestStatus()) : null,
                pageRequest,
				requestDetailsDTO.getSortBy(), requestDetailsDTO.getSortOrder()
        );

        List<RequestDetailsDTO> requestDetails = requestsPage.stream().map(this::mapToRequestDetailsDTO).collect(Collectors.toList());

        RequestDetailsResponseDTO responseDTO = new RequestDetailsResponseDTO();
        responseDTO.setTotalRecords((int) requestsPage.getTotalElements());
        responseDTO.setResults(requestDetails);

        return responseDTO;
    }

    private void validateRequestDetails(RequestDetailsDTO requestDetailsDTO) {
        if (requestDetailsDTO.getFromDate() != null && requestDetailsDTO.getFromDate().after(requestDetailsDTO.getToDate() != null ? requestDetailsDTO.getToDate() : new Timestamp(System.currentTimeMillis()))) {
            throw new CustomException("EFX_INVALID_SEARCH_FROMDATE", "From date cannot be in future");
        }

        if (requestDetailsDTO.getPageLength() < 1 || requestDetailsDTO.getPageLength() > 50) {
            throw new CustomException("EFX_INVALID_SEARCH_CRITERIA", "Invalid page length");
        }
    }

    private RequestDetailsDTO mapToRequestDetailsDTO(Request request) {
        RequestDetailsDTO requestDetailsDTO = new RequestDetailsDTO();
        requestDetailsDTO.setClientCorrelationId(request.getCorrelationId());
        requestDetailsDTO.setSourceSystem(request.getSourceSystemId());
        requestDetailsDTO.setBusinessUnit(List.of(request.getBusinessUnit()));
        requestDetailsDTO.setContractId(String.valueOf(request.getContractId()));
        requestDetailsDTO.setOrderId(String.valueOf(request.getOrderId()));
        requestDetailsDTO.setRequestStatus(String.valueOf(request.getRequestStatus()));
        return requestDetailsDTO;
    }
}
