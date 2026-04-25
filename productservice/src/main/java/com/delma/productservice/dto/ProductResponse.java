package com.delma.productservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data

public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private Integer quantity;
    private Long categoryId;
    private String imageURL;
    private Boolean active;
    private LocalDateTime createdAt;
}
