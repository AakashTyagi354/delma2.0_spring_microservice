package com.delma.aiservice.controller;


import com.delma.aiservice.agent.AgentChatRequest;
import com.delma.aiservice.agent.AgentChatResponse;
import com.delma.aiservice.agent.AgentLoop;
import com.delma.aiservice.dto.SymptomRequest;
import com.delma.aiservice.dto.SymptomResponse;
import com.delma.aiservice.rag.RagQueryService;
import com.delma.aiservice.security.AuthUtil;
import com.delma.aiservice.service.AiService;
import com.delma.common.dto.ApiResponse;
import feign.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;
    private final RagQueryService ragQueryService;
    private final AgentLoop agentLoop;
    private final AuthUtil authUtil;

    @PostMapping("/symptom-check")
    public ResponseEntity<ApiResponse<SymptomResponse>> analyzeSymptoms(@RequestBody @Valid SymptomRequest request){
        log.info("Stmptom check request reveived");
        SymptomResponse response = aiService.analyzeSymptoms(request);
        return ResponseEntity.ok(ApiResponse.success(response,"Symptom analysis complete"));
    }
    // ── RAG — Summarize patient documents for doctor ──────────────────────
    @GetMapping("/summarize/{userId}")
    public ResponseEntity<ApiResponse<String>> summarizePatientDocuments(
            @PathVariable String userId) {
        log.info("Summary request for userId: {}", userId);
        String summary = ragQueryService.summarizeForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Summary generated"));
    }

    // MCP Agent;
    @PostMapping("/agent/chat")
    public ResponseEntity<ApiResponse<AgentChatResponse>> agentChat(
            @RequestBody AgentChatRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Fallback: extract userId from JWT if header missing
        if (userId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userId = authUtil.getUserId(token);
        }

        log.info("Agent chat request from userId: {}", userId);

        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("User ID not found", "UNAUTHORIZED"));
        }

        AgentChatResponse response = agentLoop.run(
                request.getMessage(),
                request.getConversationHistory(),
                userId
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Agent response"));
    }

}
