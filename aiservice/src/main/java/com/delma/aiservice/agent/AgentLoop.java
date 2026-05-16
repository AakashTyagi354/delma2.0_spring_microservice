package com.delma.aiservice.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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
//   - 429 retry with backoff
//   - 400 correction injection — forces LLM to re-fetch real IDs
//   - Redis slot context injection — real IDs injected from Redis before LLM call
//     This solves hallucination problem: LLM no longer needs to remember IDs
// ─────────────────────────────────────────────────────────────────────────────

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLoop {

    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SlotSessionStore slotSessionStore;

    @Value("${GROQ_API_KEY}")
    private String groqApiKey;

    @Value("${GROQ_API_URL}")
    private String groqApiUrl;

    @Value("${GROQ_API_MODEL}")
    private String groqModel;

    private static final int MAX_ITERATIONS = 10;

    public AgentChatResponse run(
            String userMessage,
            List<Map<String, Object>> conversationHistory,
            String userId) {

        log.info("Agent loop started for userId: {}", userId);

        if (userMessage == null || userMessage.isBlank()) {
            return AgentChatResponse.builder()
                    .message("Please enter a message.")
                    .actionTaken(false)
                    .actionType("NONE")
                    .build();
        }

        List<Map<String, Object>> messages = new ArrayList<>();

        // System prompt — always first
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildSystemPrompt());
        messages.add(systemMsg);

        // Inject slot context from Redis if available
        // This gives LLM real doctorId and slotIds even across conversation turns
        injectSlotContext(messages, userId);

        // Add previous conversation history — text messages only
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

            // callGroq returns null when 400 correction was injected
            // Correction already added to messages — loop again
            if (response == null) {
                log.info("400 correction injected — retrying with updated messages");
                continue;
            }

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
                    String toolName   = toolCall.path("function").path("name").asText();
                    String toolArgs   = toolCall.path("function").path("arguments").asText();

                    log.info("Executing tool: {} with args: {}", toolName, toolArgs);

                    String toolResult = toolExecutor.execute(toolName, toolArgs, userId);

                    if ("book_appointment".equals(toolName)) {
                        actionTaken = true;
                        actionType = "BOOKED";
                        // Clear Redis session after successful booking
                        slotSessionStore.clearSession(userId);
                        log.info("Slot session cleared for userId: {}", userId);
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

    // ─────────────────────────────────────────────────────────────────────────
    // injectSlotContext
    //
    // Reads slot session from Redis and injects it as a system message.
    // Inserted at index 1 (right after the main system prompt).
    //
    // Why: Conversation history from frontend only has text messages.
    // Tool results with real IDs are lost between turns.
    // Redis bridges this gap — stores real doctorId + slotIds server-side.
    // ─────────────────────────────────────────────────────────────────────────
    private void injectSlotContext(List<Map<String, Object>> messages, String userId) {
        try {
            Map<String, Object> session = slotSessionStore.getSession(userId);
            if (session == null) return;

            String contextMsg = String.format(
                    "INJECTED CONTEXT — use these exact integer values for booking:\n" +
                            "doctorId: %s\n" +
                            "Available slots: %s\n" +
                            "When user picks a time, find the matching slot from above " +
                            "and use its 'id' as slotId. Both doctorId and slotId are integers.",
                    session.get("doctorId"),
                    session.get("slots")
            );

            Map<String, Object> contextInjection = new HashMap<>();
            contextInjection.put("role", "system");
            contextInjection.put("content", contextMsg);

            // Insert at index 1 — right after main system prompt
            messages.add(1, contextInjection);
            log.info("Injected slot context from Redis for userId: {}", userId);

        } catch (Exception e) {
            log.warn("Failed to inject slot context: {}", e.getMessage());
        }
    }

    private String buildSystemPrompt() {
        return """
                You are a helpful medical booking assistant for Delma Health Platform.
                Today's date is %s.
                
                You help patients find doctors and book appointments.
                
                MANDATORY WORKFLOW — never deviate from this exact order:
                Step 1: call search_doctors → get doctorId (integer)
                Step 2: call get_available_slots with that doctorId → get slot list with id (integer)
                Step 3: Show times to user and ask which they prefer
                Step 4: When user confirms, call get_available_slots AGAIN to get fresh slot IDs
                Step 5: Call book_appointment using exact integer doctorId and slotId from step 4
                
                CRITICAL RULES:
                1. NEVER use placeholder values like 123 or 456 for any ID
                2. NEVER call book_appointment without calling get_available_slots first
                3. ALWAYS call get_available_slots again before book_appointment
                   even if you already showed slots — this ensures you have real integer IDs
                4. doctorId MUST be an integer from search_doctors tool response
                5. slotId MUST be an integer from get_available_slots tool response
                6. NEVER show JSON, tool names, or IDs to the user
                7. Show slot times only — never show slotId numbers to user
                8. Always confirm doctor name, date, time before calling book_appointment
                9. After booking confirm: doctor name, date, time
                10. If INJECTED CONTEXT is present, use those exact integer values directly
                    for doctorId and slotId — do not call tools again unnecessarily
                """.formatted(LocalDate.now());
    }

    // ── Groq API call with retry logic ──────────────────────────────────────
    // Returns null when a 400 correction is injected (caller should retry)
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

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        groqApiUrl, HttpMethod.POST, entity, String.class
                );
                return objectMapper.readTree(response.getBody());

            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();

                if (status == 429 && attempt < maxRetries) {
                    int waitSeconds = attempt * 10;
                    log.warn("Rate limited by Groq. Waiting {}s before retry {}/{}",
                            waitSeconds, attempt, maxRetries);
                    try {
                        Thread.sleep(waitSeconds * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                } else if (status == 400) {
                    log.warn("LLM used wrong ID types. Injecting correction to force re-fetch.");
                    Map<String, Object> correction = new HashMap<>();
                    correction.put("role", "user");
                    correction.put("content",
                            "You must call search_doctors first, then get_available_slots " +
                                    "to get real integer IDs. Never use placeholder values. " +
                                    "Please restart the booking process from search_doctors.");
                    messages.add(correction);
                    return null;

                } else {
                    log.error("Groq API error {}: {}", status, e.getMessage());
                    throw new RuntimeException("Groq API call failed", e);
                }

            } catch (Exception e) {
                log.error("Groq API call failed: {}", e.getMessage());
                throw new RuntimeException("Groq API call failed", e);
            }
        }

        throw new RuntimeException("Groq API call failed after " + maxRetries + " retries");
    }
}