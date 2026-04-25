package com.delma.categoryservice.controller;


import com.delma.categoryservice.dto.CategoryRequest;
import com.delma.categoryservice.dto.CategoryResponse;
import com.delma.categoryservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<List<CategoryResponse>> create(@RequestBody @Valid CategoryRequest request) {
        List<CategoryResponse> allCategories = categoryService.create((request));
        return ResponseEntity.ok(allCategories);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<List<CategoryResponse>> deleteCategory(@PathVariable(name = "id") Long categoryId){
        List<CategoryResponse> deleteCategory = categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(deleteCategory);
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CategoryResponse> getBySlug(@PathVariable String slug) {
        log.info("Fetching category with slug: {}", slug);
        CategoryResponse cat =  categoryService.getBySlug(slug);
        log.info("category: {}",cat);
        return ResponseEntity.ok(cat);
    }
}
