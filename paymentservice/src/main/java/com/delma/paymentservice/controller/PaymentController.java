package com.delma.paymentservice.controller;


import com.delma.common.dto.ApiResponse;
import com.delma.paymentservice.dto.PaymentRequest;
import com.delma.paymentservice.dto.ValidateDto;
import com.delma.paymentservice.entity.IdempotencyKey;
import com.delma.paymentservice.entity.IdempotencyStatus;
import com.delma.paymentservice.service.IdempotencyService;
import com.delma.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;


@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    private static final String ENDPOINT_CREATE = "CREATE_ORDER";
    private static final String ENDPOINT_VERIFY = "VERIFY_PAYMENT";

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> initiate( @RequestHeader("Idempotency-Key") String idempotencyKey,@RequestBody PaymentRequest req) throws Exception {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<IdempotencyKey> existing =
                idempotencyService.findExistingKey(idempotencyKey,ENDPOINT_CREATE);
        if(existing.isPresent()){
            IdempotencyKey record = existing.get();
            if(record.getStatus() == IdempotencyStatus.PROCESSING){
                log.warn("Duplicate request detected for key [{}] — still PROCESSING", idempotencyKey);
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.failure("Request already in progress", "409"));
            }
            if(record.getStatus() == IdempotencyStatus.COMPLETED){
                log.info("Idempotent hit for key [{}] — returning cached response", idempotencyKey);
                ApiResponse<String> cached = objectMapper.readValue(
                        record.getResponseBody(),
                        objectMapper.getTypeFactory()
                                .constructParametricType(ApiResponse.class, String.class)
                );
                return ResponseEntity.ok(cached);
            }
            log.info("Previous attempt FAILED for key [{}] — retrying", idempotencyKey);
        }
        IdempotencyKey record =
                idempotencyService.saveAsProcessing(
                        idempotencyKey,ENDPOINT_CREATE,userId
                );
        try{

            String rzpOrderId = paymentService.createRazorpayOrder(req.getAmount(), req.getRefId());
            ApiResponse<String> response = ApiResponse.success(rzpOrderId, "rzpOrderId");
            idempotencyService.markCompleted(record,
                    objectMapper.writeValueAsString(response));

            return ResponseEntity.ok(response);
        }catch (Exception e){
            idempotencyService.markFailed(record);
            log.error("Payment order creation failed for key [{}]: {}", idempotencyKey, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verify(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody ValidateDto req) throws Exception {

        String userId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        log.info("Verify request — orderId: {}, paymentId: {}", req.getOrderId(), req.getPaymentId());

        // same pattern as /create
        Optional<IdempotencyKey> existing =
                idempotencyService.findExistingKey(idempotencyKey, ENDPOINT_VERIFY);

        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();

            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.failure("Verification already in progress", "409"));
            }


        if (record.getStatus() == IdempotencyStatus.COMPLETED) {
            log.info("Idempotent verify hit for key [{}] — returning cached", idempotencyKey);
            ApiResponse<String> cached = objectMapper.readValue(
                    record.getResponseBody(),
                    objectMapper.getTypeFactory()
                            .constructParametricType(ApiResponse.class, String.class)
            );
            return ResponseEntity.ok(cached);
        }
        }
        IdempotencyKey record =
                idempotencyService.saveAsProcessing(idempotencyKey, ENDPOINT_VERIFY, userId);

        try {
            boolean isValid = paymentService.verifySignature(
                    req.getOrderId(), req.getPaymentId(), req.getSignature());

            if (!isValid) {
                idempotencyService.markFailed(record);
                return ResponseEntity.status(400)
                        .body(ApiResponse.failure("Invalid Signature", "400"));
            }

            // TODO: Feign call to appointmentservice to confirm booking
            // (we'll add this in the next session)

            ApiResponse<String> response = ApiResponse.success("Payment Verified Successfully");
            idempotencyService.markCompleted(record,
                    objectMapper.writeValueAsString(response));

            return ResponseEntity.ok(response);

        }catch (Exception e) {
            idempotencyService.markFailed(record);
            log.error("Payment verification failed for key [{}]: {}", idempotencyKey, e.getMessage());
            throw e;
        }
    }
    }

