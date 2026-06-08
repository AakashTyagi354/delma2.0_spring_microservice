package com.delma.gateway.rateLimit;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlidingWindowStrategy implements RateLimitStrategy{
    private final ReactiveRedisTemplate<String,String> redisTemplate;
    private final SlidingWindowLuaScript luaScript;


    @Override
    public Mono<RateLimitResult> isAllowed(String key, int limit, int windowSeconds) {
        long nowMs = Instant.now().toEpochMilli();
        long windowMs = windowSeconds * 1000L;
        String requestId = UUID.randomUUID().toString();

        String redisKey = "ratelimit:" + key;

        return redisTemplate.execute(
                luaScript.getScript(),
                List.of(redisKey),
                String.valueOf(limit),
                String.valueOf(windowMs),
                String.valueOf(nowMs),
                requestId
        )
                .next()
                .map(results -> {
                    // Lua returns {allowed, remaining}
                    // results = List<Long> — raw output from Lua script
                    boolean allowed = results.get(0) == 1L;
                    long remaining = results.get(1);
                    long resetAfter = windowSeconds;
                    log.debug("Rate limit check — key: {}, allowed: {}, remaining: {}",
                            redisKey, allowed, remaining);
                    return new RateLimitResult(allowed,remaining,resetAfter);
                })
                .onErrorResume(e -> {
                    // Redis is down → FAIL OPEN (allow request)
                    // Never let Redis outage block all your users
                    log.error("Redis rate limit check failed — failing open: {}", e.getMessage());
                    return Mono.just(new RateLimitResult(true, -1, windowSeconds));
                });
    }
}
