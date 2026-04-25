package com.delma.categoryservice.dto;

import java.util.UUID;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description
) {}