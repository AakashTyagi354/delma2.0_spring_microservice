package com.delma.aiservice.agent.feign;

import com.delma.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "paymentservice")
public interface PaymentClient {

    @PostMapping("/api/v1/payments/create-order")
    ApiResponse<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> request);
}