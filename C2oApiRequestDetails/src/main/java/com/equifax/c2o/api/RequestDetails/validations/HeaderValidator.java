package com.equifax.c2o.api.RequestDetails.validation;

import com.equifax.c2o.api.RequestDetails.exception.CommonValidationException;
import com.equifax.c2o.api.RequestDetails.exception.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HeaderValidator {

    @Autowired
    private EntityManager entityManager;

    public String validateHeaders(String clientCorrelationId, String sourceSystem, List<String> businessUnits, String requestStatus) {
        List<ValidationError> validationErrors = new ArrayList<>();

        validateClientCorrelationId(clientCorrelationId, validationErrors);
        validateSourceSystem(sourceSystem, validationErrors);
        validateBusinessUnits(businessUnits, validationErrors);
        String requestCode = validateRequestStatus(requestStatus, validationErrors);

        if (!validationErrors.isEmpty()) {
            throw new CommonValidationException("EFX_HEADER_VALIDATION_FAILED", "Header validation failed", validationErrors);
        }

        return requestCode;
    }

    private void validateClientCorrelationId(String clientCorrelationId, List<ValidationError> validationErrors) {
        TypedQuery<Long> query = entityManager.createQuery(
            "SELECT COUNT(r) FROM Request r WHERE r.clientCorrelationId = :clientCorrelationId", Long.class);
        query.setParameter("clientCorrelationId", clientCorrelationId);
        Long count = query.getSingleResult();
        if (count > 0) {
            validationErrors.add(new ValidationError("EFX_DUPLICATE_CLIENT_CORRELATION_ID", "Duplicate client correlation id is not allowed"));
        }
    }

    private void validateSourceSystem(String sourceSystem, List<ValidationError> validationErrors) {
        TypedQuery<String> query = entityManager.createQuery(
            "SELECT cv.lookupValue FROM CommonLookupType ct JOIN CommonLookupValue cv " +
            "ON ct.lookupTypeId = cv.lookupTypeId WHERE ct.lookupName = 'C2O_API_CONSUMER'", String.class);
        List<String> sourceSystems = query.getResultList();
        if (!sourceSystems.contains(sourceSystem)) {
            String allowedValues = String.join(", ", sourceSystems);
            validationErrors.add(new ValidationError("EFX_INVALID_SOURCE_SYSTEM", "Allowed values for source system are " + allowedValues));
        }
    }

    private void validateBusinessUnits(List<String> businessUnits, List<ValidationError> validationErrors) {
        TypedQuery<String> query = entityManager.createQuery(
            "SELECT DISTINCT o.businessUnit FROM Organization o", String.class);
        List<String> validBusinessUnits = query.getResultList();
        for (String businessUnit : businessUnits) {
            if (!validBusinessUnits.contains(businessUnit)) {
                String allowedValues = String.join(", ", validBusinessUnits);
                validationErrors.add(new ValidationError("EFX_INVALID_BUSINESS_UNIT", "Allowed values for business unit are " + allowedValues));
            }
        }
    }

    private String validateRequestStatus(String requestStatus, List<ValidationError> validationErrors) {
        TypedQuery<Object[]> query = entityManager.createQuery(
            "SELECT c.id, c.description FROM ContractStatusLookup c " +
            "WHERE c.statusSource = 'CONTRACT_API_REQ_STATUS' AND c.enableFlag = 'Y'", Object[].class);
        List<Object[]> result = query.getResultList();
        List<String> allowedStatuses = result.stream().map(row -> (String) row[1]).collect(Collectors.toList());
        for (Object[] row : result) {
            if (row[1].equals(requestStatus)) {
                return row[0].toString(); // Return request_code as string
            }
        }

        String allowedValues = String.join(", ", allowedStatuses);
        validationErrors.add(new ValidationError("EFX_INVALID_REQUEST_STATUS", "Allowed values for request status are " + allowedValues));
        return null;
    }
}
