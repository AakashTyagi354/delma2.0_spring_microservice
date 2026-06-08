package com.delma.gateway.config;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitProperties {

    private boolean isEnable = true;
    private List<RouteLimit> routes = new ArrayList<>();

    @Getter
    @Setter
    public static class RouteLimit {
        private String path;
        private int limit;
        private int windowSeconds;
        private KeyType keyBy;
    }

    public enum KeyType {
        USER_ID, IP
    }
}