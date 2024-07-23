package com.equifax.c2o.api.RequestDetails.model;

import javax.persistence.*;

@Entity
@Table(name = "c2o_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long row_id;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "contract_id")
    private Long contractId;

    // Getters and Setters
    public Long getRowId() {
        return row_id;
    }

    public void setRowId(Long row_id) {
        this.row_id = row_id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }
}
