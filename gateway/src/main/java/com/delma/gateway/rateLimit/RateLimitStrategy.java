package com.delma.gateway.rateLimit;

import reactor.core.publisher.Mono;

public interface RateLimitStrategy {

    Mono<RateLimitResult> isAllowed(String key, int limit, int windowSeconds);

    record RateLimitResult(
            boolean allowed,
            long remaining,
            long resetAfterSeconds
    ) {}
}
