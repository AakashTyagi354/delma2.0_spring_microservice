package com.delma.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "categoryservice", url = "http://localhost:8015")
public interface CategoryClient {

    @GetMapping("/api/v1/category/{slug}")
    CategoryResponse getBySlug(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-Roles") String roles,
            @PathVariable String slug);

    record CategoryResponse(
            UUID id,
            String name,
            String slug,
            String description
    ) {}

}
