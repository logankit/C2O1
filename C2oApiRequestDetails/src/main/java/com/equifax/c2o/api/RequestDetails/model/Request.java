package com.equifax.c2o.api.RequestDetails.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "c2o_api_request")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "source_system_id")
    private String sourceSystemId;

    @Column(name = "corelation_id")
    private String correlationId;

    @Column(name = "request_status")
    private int requestStatus;

    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "le_efx_id")
    private Long leEfxId;

    @Column(name = "billing_source")
    private String billingSource;

    @Column(name = "business_unit")
    private String businessUnit;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "last_updated_date")
    private Timestamp lastUpdatedDate;

    @Column(name = "response_payload", columnDefinition = "CLOB")
    private String responsePayload;

    @Column(name = "header_payload", columnDefinition = "CLOB")
    private String headerPayload;

    @Column(name = "response_sent")
    private String responseSent;

    @Column(name = "request_payload2", columnDefinition = "CLOB")
    private String requestPayload2;

    @Column(name = "request_type")
    private String requestType;

    @Column(name = "base_contract_id")
    private Long baseContractId;

    @Column(name = "response_sent_on")
    private Timestamp responseSentOn;

    @Column(name = "redrive_flag")
    private String redriveFlag;

    @Column(name = "case_id")
    private String caseId;

    // Getters and Setters
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getSourceSystemId() {
        return sourceSystemId;
    }

    public void setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = sourceSystemId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public int getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(int requestStatus) {
        this.requestStatus = requestStatus;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public Long getLeEfxId() {
        return leEfxId;
    }

    public void setLeEfxId(Long leEfxId) {
        this.leEfxId = leEfxId;
    }

    public String getBillingSource() {
        return billingSource;
    }

    public void setBillingSource(String billingSource) {
        this.billingSource = billingSource;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Timestamp lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getHeaderPayload() {
        return headerPayload;
    }

    public void setHeaderPayload(String headerPayload) {
        this.headerPayload = headerPayload;
    }

    public String getResponseSent() {
        return responseSent;
    }

    public void setResponseSent(String responseSent) {
        this.responseSent = responseSent;
    }

    public String getRequestPayload2() {
        return requestPayload2;
    }

    public void setRequestPayload2(String requestPayload2) {
        this.requestPayload2 = requestPayload2;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public Long getBaseContractId() {
        return baseContractId;
    }

    public void setBaseContractId(Long baseContractId) {
        this.baseContractId = baseContractId;
    }

    public Timestamp getResponseSentOn() {
        return responseSentOn;
    }

    public void setResponseSentOn(Timestamp responseSentOn) {
        this.responseSentOn = responseSentOn;
    }

    public String getRedriveFlag() {
        return redriveFlag;
    }

    public void setRedriveFlag(String redriveFlag) {
        this.redriveFlag = redriveFlag;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }
}
