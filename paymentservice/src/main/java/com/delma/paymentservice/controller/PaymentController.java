package com.delma.paymentservice.controller;


import com.delma.common.dto.ApiResponse;
import com.delma.paymentservice.dto.PaymentRequest;
import com.delma.paymentservice.dto.ValidateDto;
import com.delma.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> initiate(@RequestBody PaymentRequest req) throws Exception {
        // Access variables from the object
        String rzpOrderId = paymentService.createRazorpayOrder(req.getAmount(), req.getRefId());
        return ResponseEntity.ok(ApiResponse.success(rzpOrderId,"rzpOrderId"));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@RequestBody ValidateDto req) {
        boolean isValid = paymentService.verifySignature(
                req.getOrderId(),
                req.getPaymentId(),
                req.getSignature()
        );
        if (isValid) {
            // TODO: Call AppointmentMS or OrderMS via Feign to confirm booking
            return ResponseEntity.ok(ApiResponse.success("Payment Verified Successfully"));
        }
        return ResponseEntity.status(400).body(ApiResponse.failure("Invalid Signature","400"));
    }
}
