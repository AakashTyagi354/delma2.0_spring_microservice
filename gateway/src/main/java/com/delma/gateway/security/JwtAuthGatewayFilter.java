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
        if (path.startsWith("/auth/signup") || path.startsWith("/auth/login") || path.startsWith("/auth/refresh")) {
            log.info("The enpoint is of auth do not check");
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);



        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("Token not present");
            return unauthorized(exchange);
        }

        try {
            String token = authHeader.substring(7);
            String userId;
            List<String> roles;

            log.info("token: {}", token);

            if(path.startsWith("/auth/logout")){

                userId = jwtUtil.getUserIdIgnoreExpiry(token);
                log.info("Logout endpoint for userId: {}",userId);
                roles = List.of();
            }else{
                userId = jwtUtil.getUserId(token);
                roles = jwtUtil.getRoles(token);
            }

            return chain.filter(mutateExchange(exchange,userId,roles));

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return unauthorized(exchange);
        }

    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, String userId, List<String> roles) {
        return exchange.mutate()
                .request(builder ->
                        builder.header("X-User-Id", userId)
                                .header("X-Roles", String.join(",", roles))
                ).build();

    }

    @Override
    public int getOrder() {
        return -1;
    }
}