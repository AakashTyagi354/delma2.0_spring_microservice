package com.delma.categoryservice.serviceImpl;


import com.delma.categoryservice.dto.CategoryRequest;
import com.delma.categoryservice.dto.CategoryResponse;
import com.delma.categoryservice.entity.Category;
import com.delma.categoryservice.repository.CategoryRepository;
import com.delma.categoryservice.service.CategoryService;
import com.delma.categoryservice.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;

    @Override
    public CategoryResponse create(CategoryRequest request) {
        String slug = SlugUtil.toSlug(request.name());
        if(repository.existsBySlug(slug)){
            throw new RuntimeException("Category already exists");
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        Category savedCategory = repository.save(category);
        return map(savedCategory);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return repository.findAll()
                .stream()
                .filter(Category::getIsActive)
                .map(this::map)
                .toList();
    }

    @Override
    public CategoryResponse getBySlug(String slug) {
        log.info("Looking for category with slug: {}", slug);
        return repository.findBySlugAndIsActiveTrue(slug)
                .map(this::map)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    @Override
    public CategoryResponse map(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription()
        );
    }
}
