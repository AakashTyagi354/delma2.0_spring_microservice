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

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // we do not check for auth endpoints because they are for login/signup
        if (path.startsWith("/auth/")) {
            log.info("The enpoint is of auth do not check");
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);


        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        try {
            String token = authHeader.substring(7);
            String userId = jwtUtil.getUserId(token);
            List<String> roles = jwtUtil.getRoles(token);

            // Add headers to downstream services
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(builder -> builder
                            .header("X-User-Id", userId)
                            .header("X-Roles", String.join(",", roles))
                    )
                    .build();

            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        }

    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}