package com.delma.productservice.serviceImpl;

import com.delma.common.exception.BadRequestException;
import com.delma.common.exception.ConflictException;
import com.delma.common.exception.ResourceNotFoundException;
import com.delma.productservice.client.CategoryClient;
import com.delma.productservice.dto.ProductResponse;
import com.delma.productservice.entity.Product;
import com.delma.productservice.repository.ProductRepository;
import com.delma.productservice.service.ProductService;
import com.delma.productservice.util.SlugUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;



@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final HttpServletRequest request;
    private final CategoryClient categoryClient;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket.name}") private String bucketName;

    @Override
    @Transactional
    public ProductResponse create(String name, String description, Double price, Integer quantity, Long categoryId,String categorySlug, MultipartFile photo) {
        CategoryClient.CategoryResponse categoryResponse;
        try {

            log.info("CategorySlug: {}",categorySlug);
            String token = request.getHeader("Authorization");
            categoryResponse = categoryClient.getBySlug(categorySlug,token);
            log.info("CATEGORY: {}",categoryResponse);
        }catch (Exception e) {
            log.error("Feign Call Failed. Reason: {}", e.getMessage(), e);
            throw new BadRequestException("Category fetch failed: " + e.getMessage());
        }

        String slug = SlugUtil.toSlug(name);
        if(productRepository.existsBySlug(slug)){
            throw new ConflictException("Product already exists with the provided name slug: "+slug);
        }

        String fileName = System.currentTimeMillis() + "_" + photo.getOriginalFilename();

        String fileUrl = String.format("https://%s.s3.amazonaws.com/%s",bucketName,fileName);

        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setPrice(BigDecimal.valueOf(price));
        product.setQuantity(quantity);
        product.setCategoryId(categoryId); // UUID comes from Category MS
        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setImageURL(fileUrl);


        Product saved = productRepository.save(product);

        // 2. Upload to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(photo.getContentType())
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(photo.getBytes()));
        } catch (IOException e) {
            productRepository.delete(saved);
            throw new BadRequestException("Exception will uploading the product image of AWS " + e.getMessage());
        }




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

    @Override
    public ProductResponse getSingleProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product Not found product ID: "+productId));
        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product product) {

        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setImageURL(product.getImageURL());
        response.setSlug(product.getSlug());
        response.setPrice(product.getPrice());
        response.setQuantity(product.getQuantity());
        response.setCategoryId(product.getCategoryId());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());

        return response;
    }
}
