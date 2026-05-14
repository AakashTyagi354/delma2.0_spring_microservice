package com.delma.aiservice.rag;

import com.delma.aiservice.repository.DocumentEmbeddingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
// RagQueryService
//
// Handles the query side of RAG:
// 1. Embed the query using Voyage AI
// 2. Find top K relevant chunks from pgvector
// 3. Send chunks to Groq LLM for summarization
// ─────────────────────────────────────────────────────────────────────────────

@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryService {

    private final EmbeddingService embeddingService;
    private final DocumentEmbeddingRepository embeddingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${GROQ_API_KEY}")
    private String groqApiKey;

    @Value("${GROQ_API_URL}")
    private String groqApiUrl;

    @Value("${GROQ_API_MODEL}")
    private String groqModel;

    public String summarizeForUser(String userId) {
        log.info("Generating RAG summary for userId: {}", userId);

        // Step 1 — Embed the query
        // This query captures medical document keywords
        String query = "symptoms diagnosis treatment medication prescription history";
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorStr = toVectorString(queryEmbedding);

        // Step 2 — Retrieve top 5 most relevant chunks
        List<String> relevantChunks = embeddingRepository
                .findTopKChunksByUserId(userId, vectorStr, 5);

        if (relevantChunks.isEmpty()) {
            return "No medical documents found for this patient.";
        }

        log.info("Retrieved {} relevant chunks for userId: {}", relevantChunks.size(), userId);

        // Step 3 — Build prompt with retrieved context
        String context = String.join("\n---\n", relevantChunks);
        String prompt = """
                You are a medical assistant helping a doctor prepare for a patient consultation.
                Based on the following excerpts from the patient's medical documents,
                provide a concise 3-point summary covering:
                1. Main condition or reason for visit
                2. Key symptoms or findings
                3. Current medications or treatment plan
                
                If information is not available, say "Not mentioned in documents."
                
                Patient documents:
                %s
                
                Provide ONLY the 3-point summary. Be concise and clinical.
                """.formatted(context);

        // Step 4 — Call Groq LLM
        return callGroq(prompt);
    }

    private String callGroq(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", groqModel,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 300
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    groqApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0)
                    .path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            return "Unable to generate summary at this time.";
        }
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}