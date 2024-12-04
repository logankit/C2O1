package com.equifax.c2o.api.ruleEngine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorDetail {
    private String code;
    private String message;
    private String entity;

    public ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorDetail(String code, String message, String entity) {
        this.code = code;
        this.message = message;
        this.entity = entity;
    }

    public String getFormattedEntity() {
        if (entity == null) return null;
        String[] parts = entity.split("\\[");
        if (parts.length == 2) {
            String[] subParts = parts[1].split("\\]");
            if (subParts.length == 2) {
                return parts[0] + "[" + subParts[0].trim() + "]" + subParts[1];
            }
        }
        return entity;  // No value to format
    }
}
