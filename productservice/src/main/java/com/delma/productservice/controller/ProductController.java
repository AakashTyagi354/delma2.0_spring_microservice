package com.delma.productservice.controller;

import com.delma.common.dto.ApiResponse;
import com.delma.productservice.dto.ProductResponse;
import com.delma.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/product")
@Validated
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("category") Long categoryId,
            @RequestParam("categorySlug") String categorySlug,
            @RequestParam("photo") MultipartFile photo) {


        ProductResponse newProduct = productService.create(name, description, price, quantity,categoryId,categorySlug,photo);


        return  ResponseEntity.ok(ApiResponse.success(newProduct,"Product created successfully"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductResponse> products = productService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.success(products,"Getting the products with pagination"));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductResponse> products = productService.search(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(products,"Product according to search"));
    }

    @GetMapping("/get-single-product/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getSingleProduct(@PathVariable(name = "id") Long productId){
        ProductResponse product = productService.getSingleProduct(productId);

        return ResponseEntity.ok(ApiResponse.success(product,"Getting single product"));
    }



}
