package com.emenu.features.main.controller;

import com.emenu.features.main.dto.filter.ProductFilterDto;
import com.emenu.features.main.dto.request.ProductCreateDto;
import com.emenu.features.main.dto.response.ProductDetailDto;
import com.emenu.features.main.dto.response.ProductListDto;
import com.emenu.features.main.dto.update.ProductUpdateDto;
import com.emenu.features.main.service.ProductService;
import com.emenu.shared.dto.ApiResponse;
import com.emenu.shared.dto.PaginationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PostMapping("/all")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductListDto>>> getAllProducts(
            @Valid @RequestBody ProductFilterDto filter) {
        log.info("Get all products - Page: {}, Size: {}", filter.getPageNo(), filter.getPageSize());
        PaginationResponse<ProductListDto> products = productService.getAllProducts(filter);
        return ResponseEntity.ok(ApiResponse.success(
            String.format("Found %d products", products.getTotalElements()),
            products
        ));
    }

    @PostMapping("/admin/all")
    public ResponseEntity<ApiResponse<PaginationResponse<ProductListDto>>> getAllProductAdmin(
            @Valid @RequestBody ProductFilterDto filter) {
        log.info("Get products by admin - Page: {}, Size: {}", filter.getPageNo(), filter.getPageSize());
        PaginationResponse<ProductListDto> products = productService.getAllProductsAdmin(filter);
        return ResponseEntity.ok(ApiResponse.success(
                String.format("Found %d products", products.getTotalElements()),
                products
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductById(@PathVariable UUID id) {
        log.info("Get product: {}", id);
        ProductDetailDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailDto>> createProduct(
            @Valid @RequestBody ProductCreateDto request) {
        log.info("Create product: {}", request.getName());
        ProductDetailDto product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateDto request) {
        log.info("Update product: {}", id);
        ProductDetailDto product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> deleteProduct(@PathVariable UUID id) {
        log.info("Delete product: {}", id);
        ProductDetailDto product = productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", product));
    }
}