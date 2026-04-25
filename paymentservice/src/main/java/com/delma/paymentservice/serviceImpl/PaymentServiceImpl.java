package com.delma.paymentservice.serviceImpl;

import com.delma.paymentservice.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    @Value("${razorpay.key-id}") String keyId;
    @Value("${razorpay.key-secret}") String keySecret;

    @Override
    public String createRazorpayOrder(Double amount, String referenceId) throws Exception {
        RazorpayClient client = new RazorpayClient(keyId,keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", referenceId);

        Order order = client.orders.create(orderRequest);
        log.info("Razorpay Order created: {}", order.toString());
        return order.get("id");
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            // This method will throw an exception if the signature is invalid
            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (Exception e) {
            // If an exception is caught, the signature was tampered with or is invalid
            System.err.println("Signature verification failed: " + e.getMessage());
            return false;
        }
    }
}
