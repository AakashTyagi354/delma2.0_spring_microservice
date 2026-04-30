package com.delma.paymentservice.service;



public interface PaymentService {
     String createRazorpayOrder(Double amount, String referenceId) throws Exception;
     boolean verifySignature(String orderId,String paymentId, String signature);
}
