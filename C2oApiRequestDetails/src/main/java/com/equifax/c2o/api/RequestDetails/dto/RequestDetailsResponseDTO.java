package com.equifax.c2o.api.RequestDetails.dto;

import java.util.List;

public class RequestDetailsResponseDTO {
    private int totalRecords;
    private List<RequestDetailsDTO> results;

    // Getters and Setters
    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public List<RequestDetailsDTO> getResults() {
        return results;
    }

    public void setResults(List<RequestDetailsDTO> results) {
        this.results = results;
    }
}
