package com.delma.categoryservice.controller;


import com.delma.categoryservice.dto.CategoryRequest;
import com.delma.categoryservice.dto.CategoryResponse;
import com.delma.categoryservice.service.CategoryService;
import com.delma.common.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@RequestBody @Valid CategoryRequest request) {
        CategoryResponse allCategories = categoryService.create((request));
        return ResponseEntity.ok(ApiResponse.success(allCategories,"The category has been created"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable(name = "id") Long categoryId){
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("This Category has been deleted"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAll(),"Getting all the data"));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getBySlug(@PathVariable String slug) {
        log.info("Fetching category with slug: {}", slug);
        CategoryResponse cat =  categoryService.getBySlug(slug);
        log.info("category: {}",cat);
        return ResponseEntity.ok(ApiResponse.success(cat,"Fetching category with slug"));
    }
}
