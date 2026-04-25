package com.delma.paymentservice.dto;


import lombok.Data;

@Data
public class PaymentRequest {
    private Double amount;
    private String refId;
    // Add getters and setters

    // Getters and Setters are REQUIRED for Spring to map the JSON
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }
}