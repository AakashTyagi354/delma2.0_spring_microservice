package com.delma.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "categoryservice")
public interface CategoryClient {

    @GetMapping("/api/v1/category/{slug}")
    CategoryResponse getBySlug(
            @PathVariable String slug,@RequestHeader("Authorization") String token);

    record CategoryResponse(
            Long id,
            String name,
            String slug,
            String description
    ) {}

}
