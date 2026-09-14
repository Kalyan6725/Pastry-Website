package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.product.ProductRequest;
import org.example.backend.dto.product.ProductResponse;
import org.example.backend.dto.product.ProductVariantRequest;
import org.example.backend.dto.product.ProductVariantResponse;
import org.example.backend.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/products/{id}/active")
    public ProductResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return productService.setProductActive(id, active);
    }

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVariantResponse createVariant(@PathVariable Long productId, @Valid @RequestBody ProductVariantRequest request) {
        return productService.createVariant(productId, request);
    }

    @PutMapping("/products/{productId}/variants/{variantId}")
    public ProductVariantResponse updateVariant(@PathVariable Long productId, @PathVariable Long variantId, @Valid @RequestBody ProductVariantRequest request) {
        return productService.updateVariant(productId, variantId, request);
    }

    @PatchMapping("/products/{productId}/variants/{variantId}/active")
    public ProductVariantResponse setVariantActive(@PathVariable Long productId, @PathVariable Long variantId, @RequestParam boolean active) {
        return productService.setVariantActive(productId, variantId, active);
    }
}
