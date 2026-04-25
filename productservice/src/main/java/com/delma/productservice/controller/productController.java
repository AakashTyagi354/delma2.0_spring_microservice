package com.delma.productservice.controller;

import com.delma.productservice.dto.ProductCreateRequest;
import com.delma.productservice.dto.ProductResponse;
import com.delma.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
public class productController {
    private final ProductService productService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("category") Long categoryId,
            @RequestParam("categorySlug") String categorySlug,
            @RequestParam("photo") MultipartFile photo) {

        log.info("Creating product with name: {}", name);

        ProductResponse response = productService.create(name, description, price, quantity,categoryId,categorySlug,photo);


        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductResponse> products = productService.getAll(page, size);
        return ResponseEntity.ok(products);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductResponse> products = productService.search(keyword, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/get-single-product/{id}")
    public ResponseEntity<ProductResponse> getSingleProduct(@PathVariable(name = "id") Long productId){
        ProductResponse product = productService.getSingleProduct(productId);

        return ResponseEntity.ok(product);
    }



}
