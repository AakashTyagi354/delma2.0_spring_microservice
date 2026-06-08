package com.delma.gateway.filter;

import com.delma.gateway.config.RateLimitProperties;
import com.delma.gateway.rateLimit.RateLimitStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties properties;
    private final RateLimitStrategy rateLimitStrategy;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        if (!properties.isEnable()) {
            return chain.filter(exchange);
        }

        // Use exchange.getRequest() directly — no casting
        // Never cast ServerWebExchange's request — it may be mutated
        String path = exchange.getRequest().getURI().getPath();

        RateLimitProperties.RouteLimit routeConfig = properties.getRoutes()
                .stream()
                .filter(route -> pathMatcher.match(route.getPath(), path))
                .findFirst()
                .orElse(null);

        if (routeConfig == null) {
            return chain.filter(exchange);
        }

        String rateLimitKey = buildKey(exchange, routeConfig);

        return rateLimitStrategy.isAllowed(
                rateLimitKey,
                routeConfig.getLimit(),
                routeConfig.getWindowSeconds()
        ).flatMap(result -> {

            exchange.getResponse().getHeaders()
                    .add("X-RateLimit-Limit", String.valueOf(routeConfig.getLimit()));
            exchange.getResponse().getHeaders()
                    .add("X-RateLimit-Remaining", String.valueOf(result.remaining()));
            exchange.getResponse().getHeaders()
                    .add("X-RateLimit-Reset", String.valueOf(result.resetAfterSeconds()));

            if (result.allowed()) {
                return chain.filter(exchange);
            }

            log.warn("Rate limit exceeded — key: {}, path: {}", rateLimitKey, path);
            return rateLimitExceeded(exchange, routeConfig);
        });
    }

    private String buildKey(ServerWebExchange exchange,
                            RateLimitProperties.RouteLimit config) {

        // Key by ROUTE PATTERN not actual path
        // /api/v1/ai/** → all AI endpoints share one counter
        String routeSegment = config.getPath()
                .replaceAll("[^a-zA-Z0-9]", "_");

        if (config.getKeyBy() == RateLimitProperties.KeyType.IP) {
            return "ip:" + getClientIp(exchange) + ":" + routeSegment;
        }

        String userId = exchange.getRequest()
                .getHeaders().getFirst("X-User-Id");

        if (userId == null) {
            // No userId yet (unauthenticated) — fall back to IP
            return "ip:" + getClientIp(exchange) + ":" + routeSegment;
        }

        return "user:" + userId + ":" + routeSegment;
    }

    private String getClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For first — handles nginx/load balancer
        String forwardedFor = exchange.getRequest()
                .getHeaders().getFirst("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        try {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null) {
                return remoteAddress.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            log.warn("Could not extract IP: {}", e.getMessage());
        }

        return "unknown";
    }

    private Mono<Void> rateLimitExceeded(ServerWebExchange exchange,
                                         RateLimitProperties.RouteLimit config) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> body = Map.of(
                    "success", false,
                    "message", "Too many requests. You are allowed "
                            + config.getLimit() + " requests per "
                            + config.getWindowSeconds() + " seconds.",
                    "retryAfter", config.getWindowSeconds()
            );

            byte[] bytes = objectMapper.writeValueAsBytes(body);

            return exchange.getResponse().writeWith(
                    Mono.fromCallable(() ->
                            exchange.getResponse()
                                    .bufferFactory()
                                    .wrap(bytes)
                    )
            );

        } catch (Exception e) {
            log.error("Error writing rate limit response: {}", e.getMessage());
            return exchange.getResponse().setComplete();
        }
    }
}