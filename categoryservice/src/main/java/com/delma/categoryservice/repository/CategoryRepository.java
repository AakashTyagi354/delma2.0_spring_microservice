package com.delma.categoryservice.repository;

import com.delma.categoryservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlugAndIsActiveTrue(String slug);
    Boolean existsBySlug(String slug);
}
