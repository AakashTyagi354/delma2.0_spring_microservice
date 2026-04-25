package com.delma.paymentservice.service;


import org.springframework.stereotype.Service;


public interface PaymentService {
    public String createRazorpayOrder(Double amount, String referenceId) throws Exception;
    public boolean verifySignature(String orderId,String paymentId, String signature);
}
