package com.delma.paymentservice.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

@Data
public class ValidateDto {

//    String orderId,@RequestParam String paymentId,@RequestParam String signature
    private String orderId;
    private String paymentId;
    private String signature;


}
