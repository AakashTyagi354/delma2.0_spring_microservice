package com.delma.aiservice.agent;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentChatResponse {
    private String message;
    private boolean actionTaken;
    private String actionType;
    // Payment fields — only populated when actionType = "PAYMENT_REQUIRED"
    private String razorpayOrderId;
    private Integer amount;
    private Long doctorId;
    private Long slotId;
}
