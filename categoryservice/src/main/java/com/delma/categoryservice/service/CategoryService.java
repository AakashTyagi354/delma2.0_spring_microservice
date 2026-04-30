package com.delma.categoryservice.service;

import com.delma.categoryservice.dto.CategoryRequest;
import com.delma.categoryservice.dto.CategoryResponse;
import com.delma.categoryservice.entity.Category;

import java.util.List;

public interface CategoryService {
      CategoryResponse  create(CategoryRequest request);
      List<CategoryResponse> getAll();
      CategoryResponse getBySlug(String slug);
      void deleteCategory(Long categoryId);
}
