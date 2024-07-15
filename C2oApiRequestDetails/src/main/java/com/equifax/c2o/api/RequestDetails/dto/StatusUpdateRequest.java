package com.equifax.c2o.api.RequestDetails.dto;

public class StatusUpdateRequest {
    private String rootCorrelationId;
    private String status;

    // Getters and Setters
    public String getRootCorrelationId() {
        return rootCorrelationId;
    }

    public void setRootCorrelationId(String rootCorrelationId) {
        this.rootCorrelationId = rootCorrelationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
