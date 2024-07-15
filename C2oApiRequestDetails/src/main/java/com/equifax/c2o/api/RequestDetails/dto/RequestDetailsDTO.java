package com.equifax.c2o.api.RequestDetails.dto;

import java.sql.Timestamp;
import java.util.List;

public class RequestDetailsDTO {
    private String clientCorrelationId;
    private String sourceSystem;
    private Timestamp fromDate;
    private Timestamp toDate;
    private List<String> businessUnit;
    private String contractId;
    private String orderId;
    private String requestStatus;
    private int startIndex = 0;
    private int pageLength = 20;

    // Getters and Setters
    public String getClientCorrelationId() {
        return clientCorrelationId;
    }

    public void setClientCorrelationId(String clientCorrelationId) {
        this.clientCorrelationId = clientCorrelationId;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public Timestamp getFromDate() {
        return fromDate;
    }

    public void setFromDate(Timestamp fromDate) {
        this.fromDate = fromDate;
    }

    public Timestamp getToDate() {
        return toDate;
    }

    public void setToDate(Timestamp toDate) {
        this.toDate = toDate;
    }

    public List<String> getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(List<String> businessUnit) {
        this.businessUnit = businessUnit;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getPageLength() {
        return pageLength;
    }

    public void setPageLength(int pageLength) {
        this.pageLength = pageLength;
    }
}
