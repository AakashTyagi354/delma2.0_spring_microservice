package com.delma.common.config;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

        return builder.routes()

                // ================= USER SERVICE =================
                .route("user-auth-route", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8011")
                )

                .route("user-admin-route", r -> r
                        .path("/api/v1/admin/**")
                        .uri("http://localhost:8011")
                )

                .route("user-route", r -> r
                        .path("/api/v1/user/**")
                        .uri("http://localhost:8011")
                )

                // ================= DOCTOR SERVICE =================
                .route("doctor-route", r -> r
                        .path("/api/v1/doctor/**")
                        .uri("http://localhost:8010")
                )

                // ================= APPOINTMENT SERVICE =================
                .route("appointment-route", r -> r
                        .path("/api/v1/appointments/**")
                        .uri("http://localhost:8020")
                )

                .build();
    }

}
