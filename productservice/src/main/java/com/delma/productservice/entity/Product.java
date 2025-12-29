package com.delma.productservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String slug;

    @Column(length = 1000)
    private String description;

    private BigDecimal price;
    private Integer quantity;

    private UUID categoryId;

    private Boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
