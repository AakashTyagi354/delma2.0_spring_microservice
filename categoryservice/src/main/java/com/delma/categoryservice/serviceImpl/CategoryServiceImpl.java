package com.delma.categoryservice.serviceImpl;


import com.delma.categoryservice.dto.CategoryRequest;
import com.delma.categoryservice.dto.CategoryResponse;
import com.delma.categoryservice.entity.Category;
import com.delma.categoryservice.repository.CategoryRepository;
import com.delma.categoryservice.service.CategoryService;
import com.delma.categoryservice.util.SlugUtil;
import com.delma.common.exception.ConflictException;
import com.delma.common.exception.ResourceNotFoundException;
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
            throw new ConflictException("Category already exists slug: "+slug);
        }

        Category category = new Category();
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        Category savedCategory = repository.save(category);
        return toResponse(savedCategory);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return repository.findAll()
                .stream()
                .filter(Category::getIsActive)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getBySlug(String slug) {
        log.info("Looking for category with slug: {}", slug);
        return repository.findBySlugAndIsActiveTrue(slug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found slug: "+slug));
    }


    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription()
        );
    }

    @Override
    public void deleteCategory(Long categoryId) {
       Category category =  repository.findById(categoryId)  // DB call 1 - SELECT
                .orElseThrow(() -> new ResourceNotFoundException("Category not found categoryId "+categoryId));
        repository.delete(category);
    }
}

