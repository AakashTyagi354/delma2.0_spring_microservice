package com.delma.aiservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
// SlotSessionStore
//
// Stores slot context in Redis per user session.
// Solves the LLM hallucination problem — LLM doesn't need to remember IDs.
// When user picks a time, we look up the real slotId from Redis.
//
// Key: agent:slots:{userId}
// TTL: 10 minutes
// ─────────────────────────────────────────────────────────────────────────────

@Slf4j
@Component
@RequiredArgsConstructor
public class SlotSessionStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration TTL = Duration.ofMinutes(10);

    // Store slot list and doctorId for a user
    public void storeSlots(String userId, Long doctorId, List<Map<String, Object>> slots) {
        try {
            Map<String, Object> session = Map.of(
                    "doctorId", doctorId,
                    "slots", slots
            );
            String key = "agent:slots:" + userId;
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(session), TTL);
            log.info("Stored {} slots for userId: {}", slots.size(), userId);
        } catch (Exception e) {
            log.error("Failed to store slots in Redis: {}", e.getMessage());
        }
    }

    // Get stored session for user
    public Map<String, Object> getSession(String userId) {
        try {
            String key = "agent:slots:" + userId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to get slots from Redis: {}", e.getMessage());
            return null;
        }
    }

    // Clear session after booking
    public void clearSession(String userId) {
        redisTemplate.delete("agent:slots:" + userId);
    }
}