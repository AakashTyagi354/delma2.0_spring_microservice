package com.delma.aiservice.controller;


import com.delma.aiservice.dto.SymptomRequest;
import com.delma.aiservice.dto.SymptomResponse;
import com.delma.aiservice.service.AiService;
import com.delma.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @PostMapping("/symptom-check")
    public ResponseEntity<ApiResponse<SymptomResponse>> analyzeSymptoms(@RequestBody @Valid SymptomRequest request){
        log.info("Stmptom check request reveived");
        SymptomResponse response = aiService.analyzeSymptoms(request);
        return ResponseEntity.ok(ApiResponse.success(response,"Symptom analysis complete"));
    }

}
