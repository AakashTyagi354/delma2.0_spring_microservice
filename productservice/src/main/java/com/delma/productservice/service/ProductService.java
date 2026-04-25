package com.delma.productservice.service;

import com.delma.productservice.dto.ProductCreateRequest;
import com.delma.productservice.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
        public ProductResponse create(String name, String description, Double price, Integer quantity, Long categoryId,String categorySlug, MultipartFile photo);
        public Page<ProductResponse> search(String keyword, int page, int size);
        public Page<ProductResponse> getAll(int page, int size);
        public ProductResponse getSingleProduct(Long productId);
}
