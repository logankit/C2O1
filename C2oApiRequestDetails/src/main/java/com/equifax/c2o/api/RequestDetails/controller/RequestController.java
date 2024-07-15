package com.equifax.c2o.api.RequestDetails.controller;

import com.equifax.c2o.api.RequestDetails.dto.RequestDetailsDTO;
import com.equifax.c2o.api.RequestDetails.dto.RequestDetailsResponseDTO;
import com.equifax.c2o.api.RequestDetails.dto.StatusUpdateRequest;
import com.equifax.c2o.api.RequestDetails.exception.CustomHeaderValidationException;
import com.equifax.c2o.api.RequestDetails.service.RequestDetailsService;
import com.equifax.c2o.api.RequestDetails.service.PayloadService;
import com.equifax.c2o.api.RequestDetails.service.StatusUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/requests")
public class RequestController {

    @Autowired
    private RequestDetailsService requestDetailsService;

    @Autowired
    private StatusUpdateService statusUpdateService;

    @Autowired
    private PayloadService payloadService;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping
    public ResponseEntity<RequestDetailsResponseDTO> getRequestDetails(
            @RequestHeader("ClientCorrelationId") String clientCorrelationId,
            @RequestHeader("SourceSystem") String sourceSystem,
            @RequestParam(required = false) Long fromDate,
            @RequestParam(required = false) Long toDate,
            @RequestParam(required = false) String[] businessUnit,
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String requestStatus,
            @RequestParam(defaultValue = "0") int startIndex,
            @RequestParam(defaultValue = "20") int pageLength) {

        validateRequestHeaders(clientCorrelationId, sourceSystem, businessUnit, requestStatus);

        RequestDetailsDTO requestDetailsDTO = new RequestDetailsDTO();
        requestDetailsDTO.setClientCorrelationId(clientCorrelationId);
        requestDetailsDTO.setSourceSystem(sourceSystem);
        requestDetailsDTO.setBusinessUnit(List.of(businessUnit));
        requestDetailsDTO.setContractId(contractId);
        requestDetailsDTO.setOrderId(orderId);
        requestDetailsDTO.setRequestStatus(requestStatus);
        requestDetailsDTO.setStartIndex(startIndex);
        requestDetailsDTO.setPageLength(pageLength);

        RequestDetailsResponseDTO response = requestDetailsService.getRequestDetails(requestDetailsDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody StatusUpdateRequest statusUpdateRequest) {
        statusUpdateService.updateStatus(statusUpdateRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payload")
    public ResponseEntity<PayloadResponseDTO> processPayload(@RequestBody String payload) {
        PayloadResponseDTO response = payloadService.processPayload(payload);
        return ResponseEntity.ok(response);
    }

    private void validateRequestHeaders(String clientCorrelationId, String sourceSystem, String[] businessUnit, String requestStatus) {
        List<CustomHeaderValidationException.CustomException> errors = new ArrayList<>();

        validateClientCorrelationId(clientCorrelationId, errors);
        validateSourceSystem(sourceSystem, errors);
        validateBusinessUnit(businessUnit, errors);
        validateRequestStatus(requestStatus, errors);

        if (!errors.isEmpty()) {
            throw new CustomHeaderValidationException(errors);
        }
    }

    private void validateClientCorrelationId(String clientCorrelationId, List<CustomHeaderValidationException.CustomException> errors) {
        Query query = entityManager.createQuery("SELECT COUNT(c) FROM Request c WHERE c.correlationId = :clientCorrelationId");
        query.setParameter("clientCorrelationId", clientCorrelationId);
        Long count = (Long) query.getSingleResult();
        if (count > 0) {
            errors.add(new CustomHeaderValidationException.CustomException("EFX_DUPLICATE_CLIENT_CORRELATION_ID", "Duplicate client correlation id is not allowed"));
        }
    }

    private void validateSourceSystem(String sourceSystem, List<CustomHeaderValidationException.CustomException> errors) {
        Query query = entityManager.createQuery("SELECT cv.lookupValue FROM CommonLookupTypes ct, CommonLookupValues cv WHERE ct.lookupTypeId = cv.lookupTypeId AND ct.lookupName = 'C2O_API_CONSUMER'");
        List<String> sourceSystems = query.getResultList();
        if (!sourceSystems.contains(sourceSystem)) {
            String allowedValues = String.join(", ", sourceSystems);
            errors.add(new CustomHeaderValidationException.CustomException("EFX_INVALID_SOURCE_SYSTEM", "Allowed values for source system are " + allowedValues));
        }
    }

    private void validateBusinessUnit(String[] businessUnits, List<CustomHeaderValidationException.CustomException> errors) {
        Query query = entityManager.createQuery("SELECT DISTINCT bu FROM Organization");
        List<String> validBusinessUnits = query.getResultList();
        for (String bu : businessUnits) {
            if (!validBusinessUnits.contains(bu)) {
                String allowedValues = String.join(", ", validBusinessUnits);
                errors.add(new CustomHeaderValidationException.CustomException("EFX_INVALID_BUSINESS_UNIT", "Allowed values for business unit are " + allowedValues));
            }
        }
    }

    private void validateRequestStatus(String requestStatus, List<CustomHeaderValidationException.CustomException> errors) {
        Query query = entityManager.createQuery("SELECT cs.id, cs.description FROM ContractStatusLookup cs WHERE cs.statusSource = 'CONTRACT_API_REQ_STATUS' AND cs.enableFlag = 'Y'");
        List<Object[]> results = query.getResultList();

        List<String> validStatuses = results.stream().map(result -> (String) result[1]).collect(Collectors.toList());
        if (!validStatuses.contains(requestStatus)) {
            String allowedValues = String.join(", ", validStatuses);
            errors.add(new CustomHeaderValidationException.CustomException("EFX_INVALID_REQUEST_STATUS", "Allowed values for request status are " + allowedValues));
        }

        // Replace requestStatus with requestCode
        String requestCode = results.stream().filter(result -> result[1].equals(requestStatus)).map(result -> (String) result[0]).findFirst().orElse(null);
        if (requestCode != null) {
            requestStatus = requestCode;
        }
    }
}
