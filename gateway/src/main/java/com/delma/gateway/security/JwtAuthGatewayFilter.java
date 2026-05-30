package com.delma.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // ── Generate correlationId ────────────────────────────────────────
        // Use existing one if client sent it (useful for retries)
        // Otherwise generate a fresh UUID for this request
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // ── Add to response so frontend can read it ───────────────────────
        // If booking fails, user sees correlationId in response headers
        // They give it to support → you search Grafana → instant diagnosis
        exchange.getResponse()
                .getHeaders()
                .add(CORRELATION_ID_HEADER, finalCorrelationId);

        // ── Auth bypass for public endpoints ─────────────────────────────
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/auth/verify-otp")
                || path.startsWith("/auth/resend-otp")
                || path.startsWith("/auth/admin-login")
                || path.startsWith("/auth/signup")
                || path.startsWith("/auth/login")
                || path.startsWith("/auth/refresh")) {

            log.info("Public endpoint — skipping JWT check. correlationId={}",
                    finalCorrelationId);

            // Still forward correlationId even for public endpoints
            return chain.filter(
                    mutateExchange(exchange, null, List.of(), finalCorrelationId)
            );
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No token present. correlationId={}", finalCorrelationId);
            return unauthorized(exchange);
        }

        try {
            String token = authHeader.substring(7);
            String userId;
            List<String> roles;

            if (path.startsWith("/auth/logout")) {
                userId = jwtUtil.getUserIdIgnoreExpiry(token);
                log.info("Logout for userId={}. correlationId={}",
                        userId, finalCorrelationId);
                roles = List.of();
            } else {
                userId = jwtUtil.getUserId(token);
                roles = jwtUtil.getRoles(token);
            }

            return chain.filter(
                    mutateExchange(exchange, userId, roles, finalCorrelationId)
            );

        } catch (Exception e) {
            log.error("JWT validation failed: {}. correlationId={}",
                    e.getMessage(), finalCorrelationId);
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // ── Updated mutateExchange — now includes correlationId ──────────────
    // This is what passes the correlationId downstream to every microservice
    // Each service reads it from X-Correlation-Id header
    // CorrelationIdFilter in each service puts it in MDC
    // Every log line from every service now includes this same ID
    private ServerWebExchange mutateExchange(
            ServerWebExchange exchange,
            String userId,
            List<String> roles,
            String correlationId) {

        return exchange.mutate()
                .request(builder -> {
                    builder.header(CORRELATION_ID_HEADER, correlationId);
                    if (userId != null) {
                        builder.header("X-User-Id", userId);
                        builder.header("X-Roles", String.join(",", roles));
                    }
                })
                .build();
    }

    @Override
    public int getOrder() {
        return -1; // runs before all other filters
    }
}