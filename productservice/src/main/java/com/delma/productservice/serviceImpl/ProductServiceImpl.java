package com.delma.productservice.serviceImpl;

import com.delma.productservice.client.CategoryClient;
import com.delma.productservice.dto.ProductCreateRequest;
import com.delma.productservice.dto.ProductResponse;
import com.delma.productservice.entity.Product;
import com.delma.productservice.repository.ProductRepository;
import com.delma.productservice.service.ProductService;
import com.delma.productservice.util.SlugUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryClient categoryClient;

    @Override
    public ProductResponse create(ProductCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String userId = auth.getName();  // what you set in GatewayHeaderAuthFilter
        String roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .collect(Collectors.joining(","));
        CategoryClient.CategoryResponse categoryResponse;
        try {
            categoryResponse = categoryClient.getBySlug(userId,roles,request.getCategorySlug());
        } catch (Exception e) {
            throw new IllegalArgumentException("Category not found");
        }

        String slug = SlugUtil.toSlug(request.getName());
        if(productRepository.existsBySlug(slug)){
            throw new IllegalArgumentException("Product already exists with the provided name");
        }
        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(slug);
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategoryId(categoryResponse.id()); // UUID comes from Category MS
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        return mapToResponse(saved);

    }

    @Override
    public Page<ProductResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page,size);
        return productRepository.search(keyword.toLowerCase(),pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<ProductResponse> getAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    private ProductResponse mapToResponse(Product product) {

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setSlug(product.getSlug());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setCategoryId(product.getCategoryId());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());

        return response;
    }
}
