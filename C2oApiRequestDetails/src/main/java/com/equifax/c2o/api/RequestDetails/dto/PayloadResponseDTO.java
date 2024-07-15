package com.equifax.c2o.api.RequestDetails.dto;

public class PayloadResponseDTO {
    private String requestPayload;
    private String responsePayload;

    // Getters and Setters
    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }
}
