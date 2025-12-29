package com.delma.categoryservice.service;

import com.delma.categoryservice.dto.CategoryRequest;
import com.delma.categoryservice.dto.CategoryResponse;
import com.delma.categoryservice.entity.Category;

import java.util.List;

public interface CategoryService {
    public CategoryResponse create(CategoryRequest request);
    public List<CategoryResponse> getAll();
    public CategoryResponse getBySlug(String slug);
    public CategoryResponse map(Category category);
}
