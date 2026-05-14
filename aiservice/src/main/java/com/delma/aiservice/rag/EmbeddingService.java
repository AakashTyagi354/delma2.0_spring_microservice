package com.delma.aiservice.rag;

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
public class EmbeddingService {

    @Value("${VOYAGE_API_KEY}")
    private String apiKey;

    private static final String EMBEDDING_URL = "https://api.voyageai.com/v1/embeddings";
    private static final String EMBEDDING_MODEL = "voyage-3-lite";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;


    public float[] embed(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = Map.of(
                "input", List.of(text),
                "model", EMBEDDING_MODEL
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Rate limit: 3 RPM without payment method
            // Wait 21 seconds between calls to stay safely under limit
            Thread.sleep(21000);

            ResponseEntity<String> response = restTemplate.exchange(
                    EMBEDDING_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            return parseEmbedding(response.getBody());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Embedding interrupted", e);
        } catch (Exception e) {
            log.error("Embedding API call failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    private float[] parseEmbedding(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Voyage response: { "data": [{ "embedding": [...] }] }
            JsonNode embeddingArray = root.path("data").get(0).path("embedding");

            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = (float) embeddingArray.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            log.error("Failed to parse embedding response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse embedding", e);
        }
    }
}