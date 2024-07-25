package com.equifax.c2o.api.RequestDetails.controller;

import com.equifax.c2o.api.RequestDetails.dto.RequestDetailsDTO;
import com.equifax.c2o.api.RequestDetails.dto.RequestDetailsResponseDTO;
import com.equifax.c2o.api.RequestDetails.dto.PayloadResponseDTO;
import com.equifax.c2o.api.RequestDetails.dto.StatusUpdateRequest;
import com.equifax.c2o.api.RequestDetails.service.RequestDetailsService;
import com.equifax.c2o.api.RequestDetails.service.StatusUpdateService;
import com.equifax.c2o.api.RequestDetails.service.PayloadService;
import com.equifax.c2o.api.RequestDetails.validation.HeaderValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Arrays;

@RestController
@RequestMapping("/requests")
public class RequestController {

    @Autowired
    private RequestDetailsService requestDetailsService;

    @Autowired
    private StatusUpdateService statusUpdateService;

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private HeaderValidator headerValidator;

    @GetMapping
    public RequestDetailsResponseDTO getRequestDetails(
            @RequestHeader("ClientCorrelationId") String clientCorrelationId,
            @RequestHeader("SourceSystem") String sourceSystem,
            @RequestParam(required = false) Long fromDate,
            @RequestParam(required = false) Long toDate,
            @RequestParam(required = false) String[] businessUnit,
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String requestStatus,
            @RequestParam(defaultValue = "0") int startIndex,
            @RequestParam(defaultValue = "20") int pageLength
			@RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder
			) {

        String requestCode = headerValidator.validateHeaders(clientCorrelationId, sourceSystem, businessUnit != null ? Arrays.asList(businessUnit) : null, requestStatus);

        RequestDetailsDTO requestDetailsDTO = new RequestDetailsDTO();
        requestDetailsDTO.setClientCorrelationId(clientCorrelationId);
        requestDetailsDTO.setSourceSystem(sourceSystem);
        requestDetailsDTO.setFromDate(fromDate != null ? new Timestamp(fromDate) : null);
        requestDetailsDTO.setToDate(toDate != null ? new Timestamp(toDate) : null);
        requestDetailsDTO.setBusinessUnit(businessUnit != null ? Arrays.asList(businessUnit) : null);
        requestDetailsDTO.setContractId(contractId);
        requestDetailsDTO.setOrderId(orderId);
        requestDetailsDTO.setRequestStatus(requestCode != null ? Integer.parseInt(requestCode) : null); // Set requestCode as number
        requestDetailsDTO.setStartIndex(startIndex);
        requestDetailsDTO.setPageLength(pageLength);
		 requestDetailsDTO.setSortBy(sortBy);
        requestDetailsDTO.setSortOrder(sortOrder);

        RequestDetailsResponseDTO response = requestDetailsService.getRequestDetails(requestDetailsDTO);

        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<String> updateStatus(
            @RequestHeader("ClientCorrelationId") String clientCorrelationId,
            @RequestHeader("SourceSystem") String sourceSystem,
            @RequestBody StatusUpdateRequest statusUpdateRequest) {

        headerValidator.validateHeaders(clientCorrelationId, sourceSystem, null);

        statusUpdateService.updateStatus(statusUpdateRequest);
        return ResponseEntity.ok("CAPI Status update received successfully");
    }

    @GetMapping("/{correlationId}/payload")
    public ResponseEntity<PayloadResponseDTO> getPayload(
            @PathVariable("correlationId") String correlationId,
            @RequestHeader("ClientCorrelationId") String clientCorrelationId,
            @RequestHeader("SourceSystem") String sourceSystem,
            @RequestHeader(value = "ReqType", defaultValue = "RequestPayload") String reqType) {

        headerValidator.validateHeaders(clientCorrelationId, sourceSystem, null);

        PayloadResponseDTO payloadResponseDTO = payloadService.getPayload(correlationId, reqType);
        return ResponseEntity.ok(payloadResponseDTO);
    }
}
