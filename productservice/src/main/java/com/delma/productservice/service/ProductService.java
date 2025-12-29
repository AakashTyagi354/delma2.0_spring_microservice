package com.delma.productservice.service;

import com.delma.productservice.dto.ProductCreateRequest;
import com.delma.productservice.dto.ProductResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
        public ProductResponse create(ProductCreateRequest request);
        public Page<ProductResponse> search(String keyword, int page, int size);
        public Page<ProductResponse> getAll(int page, int size);
}
