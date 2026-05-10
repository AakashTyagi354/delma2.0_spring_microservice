package com.delma.aiservice.serviceImpl;

import com.delma.aiservice.dto.SymptomRequest;
import com.delma.aiservice.dto.SymptomResponse;
import com.delma.aiservice.service.AiService;
import com.delma.common.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    @Value("${GROQ_API_KEY}")
    private String apiKey;

    @Value("${GROQ_API_URL}")
    private String apiUrl;

    @Value("${GROQ_API_MODEL}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public SymptomResponse analyzeSymptoms(SymptomRequest request) {
        log.info("Analyzing symptoms: {}", request.symptoms());
        String prompt = buildPrompt(request.symptoms());
        String rawResponse = callGeminiApi(prompt);
        return parseResponse(rawResponse);
    }

    private String buildPrompt(String symptoms) {
        return """
                You are a medical triage assistant for Delma Health platform.
                A patient has described the following symptoms: "%s"
                
                Based on these symptoms, identify the most appropriate medical specialization.
                
                You MUST respond with ONLY a valid JSON object in this exact format, no other text:
                {
                    "specialization": "<medical specialization>",
                    "message": "<one sentence explanation why this specialization>",
                    "disclaimer": "This is not a medical diagnosis. Please consult a qualified doctor."
                }
                
                Valid specializations: Cardiology, Neurology, Orthopedics, Dermatology, Pediatrics,
                General Surgery, Ophthalmology, Psychiatry, Gynecology, Urology, ENT, Gastroenterology,
                Pulmonology, Endocrinology, General Medicine.
                
                If symptoms are unclear or too vague, use General Medicine.
                Respond with ONLY the JSON object. No markdown. No explanation. No backticks.
                """.formatted(symptoms);
    }

    private String callGeminiApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 200
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            log.info("Groq API response received");
            return extractTextFromResponse(response.getBody());
        } catch (Exception e) {
            log.error("Groq API call failed. Type: {}, Message: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw new BadRequestException("AI service temporarily unavailable. Please try again.");
        }
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception e) {
            log.error("Failed to parse API response: {}", e.getMessage());
            throw new BadRequestException("Failed to process AI response.");
        }
    }
    private SymptomResponse parseResponse(String rawJson) {
        try {
            String cleanJson = rawJson.trim()
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(cleanJson);

            return new SymptomResponse(
                    node.path("specialization").asText("General Medicine"),
                    node.path("message").asText("Please consult a doctor for proper diagnosis."),
                    node.path("disclaimer").asText("This is not a medical diagnosis. Please consult a qualified doctor.")
            );
        } catch (Exception e) {
            log.error("Failed to parse symptom response: {}", e.getMessage());
            return new SymptomResponse(
                    "General Medicine",
                    "We could not analyze your symptoms. Please consult a General Medicine doctor.",
                    "This is not a medical diagnosis. Please consult a qualified doctor."
            );
        }
    }
}