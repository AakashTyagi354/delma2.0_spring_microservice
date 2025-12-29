package com.delma.productservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProductResponse {
    private UUID id;
    private String name;
    private String slug;
    private BigDecimal price;
    private Integer quantity;
    private UUID categoryId;
    private Boolean active;
    private LocalDateTime createdAt;
}
