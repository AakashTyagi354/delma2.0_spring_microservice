package com.delma.productservice.service;

import com.delma.productservice.dto.ProductCreateRequest;
import com.delma.productservice.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
          ProductResponse create(String name, String description, Double price, Integer quantity, Long categoryId,String categorySlug, MultipartFile photo);
          Page<ProductResponse> search(String keyword, int page, int size);
          Page<ProductResponse> getAll(int page, int size);
          ProductResponse getSingleProduct(Long productId);
}
