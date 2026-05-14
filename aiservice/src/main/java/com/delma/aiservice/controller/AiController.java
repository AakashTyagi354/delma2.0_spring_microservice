package com.delma.aiservice.controller;


import com.delma.aiservice.dto.SymptomRequest;
import com.delma.aiservice.dto.SymptomResponse;
import com.delma.aiservice.rag.RagQueryService;
import com.delma.aiservice.service.AiService;
import com.delma.common.dto.ApiResponse;
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

}
