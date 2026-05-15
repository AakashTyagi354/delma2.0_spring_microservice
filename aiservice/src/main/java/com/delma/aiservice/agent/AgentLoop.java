package com.delma.aiservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

// ─────────────────────────────────────────────────────────────────────────────
// AgentLoop
//
// Core of the MCP agent. Implements the ReAct pattern:
// Reason → Act → Observe → Reason → Act → ...
//
// Fixes applied:
//   - tool_Calls typo → tool_calls
//   - Map.of() → HashMap everywhere (Map.of throws NPE on null values)
//   - Null safety for userMessage and tool results
//   - AgentChatResponse uses builder pattern (has 7 fields now)
// ─────────────────────────────────────────────────────────────────────────────

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLoop {

    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${GROQ_API_KEY}")
    private String groqApiKey;

    @Value("${GROQ_API_URL}")
    private String groqApiUrl;

    @Value("${GROQ_API_MODEL}")
    private String groqModel;

    private static final int MAX_ITERATIONS = 8;

    public AgentChatResponse run(
            String userMessage,
            List<Map<String, Object>> conversationHistory,
            String userId) {

        log.info("Agent loop started for userId: {}", userId);

        // Null safety
        if (userMessage == null || userMessage.isBlank()) {
            return AgentChatResponse.builder()
                    .message("Please enter a message.")
                    .actionTaken(false)
                    .actionType("NONE")
                    .build();
        }

        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt());
        messages.add(systemMsg);

        // Add previous conversation history
        if (conversationHistory != null) {
            conversationHistory.stream()
                    .filter(m -> m != null
                            && m.get("role") != null
                            && m.get("content") != null)
                    .forEach(messages::add);
        }

        // Add current user message
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        boolean actionTaken = false;
        String actionType = "NONE";

        // ── Agent Loop ──────────────────────────────────────────────────────
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            log.info("Agent loop iteration: {}", i + 1);

            JsonNode response = callGroq(messages);
            JsonNode choice = response.path("choices").get(0);
            String finishReason = choice.path("finish_reason").asText();
            JsonNode message = choice.path("message");

            if ("tool_calls".equals(finishReason)) {

                JsonNode toolCalls = message.path("tool_calls");

                List<Object> toolCallsList =
                        toolCalls.isNull() || toolCalls.isMissingNode()
                                ? List.of()
                                : objectMapper.convertValue(toolCalls, List.class);

                Map<String, Object> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("tool_calls", toolCallsList);
                messages.add(assistantMsg);

                for (JsonNode toolCall : toolCalls) {
                    String toolCallId = toolCall.path("id").asText();
                    String toolName   = toolCall.path("function")
                            .path("name").asText();
                    String toolArgs   = toolCall.path("function")
                            .path("arguments").asText();

                    log.info("Executing tool: {} with args: {}", toolName, toolArgs);

                    String toolResult = toolExecutor.execute(toolName, toolArgs, userId);

                    if ("book_appointment".equals(toolName)) {
                        actionTaken = true;
                        actionType = "BOOKED";
                    }

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", toolCallId);
                    toolMsg.put("content", toolResult != null ? toolResult : "");
                    messages.add(toolMsg);
                }

            } else if ("stop".equals(finishReason)) {

                String finalAnswer = message.path("content").asText();
                log.info("Agent loop complete after {} iterations", i + 1);
                return AgentChatResponse.builder()
                        .message(finalAnswer)
                        .actionTaken(actionTaken)
                        .actionType(actionType)
                        .build();

            } else {
                log.warn("Unexpected finish_reason: {}", finishReason);
                break;
            }
        }

        return AgentChatResponse.builder()
                .message("I was unable to complete the request. Please try again.")
                .actionTaken(false)
                .actionType("NONE")
                .build();
    }

    private String buildSystemPrompt() {
        return """
                You are a helpful medical booking assistant for Delma Health Platform.
                Today's date is %s.
                
                You help patients:
                - Find doctors by specialization
                - Check available appointment slots
                - Book appointments
                - View their upcoming appointments
                
                STRICT RULES — follow these exactly:
                1. NEVER show tool names, function calls, or JSON to the user
                2. NEVER make up doctor IDs or slot IDs — only use IDs from tool results
                3. Always search doctors first, then get slots, then book
                4. When showing slots to user, show time only — never show slotId
                5. When user picks a time, find the matching slotId from the tool result
                   and use that exact slotId when calling book_appointment
                6. Always confirm doctor name, date and time before booking
                7. If a slot is not found, say so clearly and offer alternatives
                8. After successful booking confirm with: doctor name, date, time
                
                IMPORTANT: You have access to real data via tools.
                Never guess or hallucinate IDs — always use exact values from tool responses.
                """.formatted(LocalDate.now());
    }

    private JsonNode callGroq(List<Map<String, Object>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        List<Map<String, Object>> tools = objectMapper.convertValue(
                toolRegistry.getTools(),
                objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, Map.class)
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", groqModel);
        body.put("messages", messages);
        body.put("tools", tools);
        body.put("tool_choice", "auto");
        body.put("max_tokens", 500);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    groqApiUrl, HttpMethod.POST, entity, String.class
            );
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new RuntimeException("Groq API call failed", e);
        }
    }
}