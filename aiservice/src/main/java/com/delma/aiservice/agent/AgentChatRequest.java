package com.delma.aiservice.agent;


import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentChatRequest {
    private String message;

    // Frontend sends full history for multi-turn conversation
    private List<Map<String,Object>> conversationHistory;

}
