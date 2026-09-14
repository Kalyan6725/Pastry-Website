package org.example.backend.service;

import org.example.backend.dto.product.ProductRequest;
import org.example.backend.dto.product.ProductResponse;
import org.example.backend.dto.product.ProductVariantRequest;
import org.example.backend.dto.product.ProductVariantResponse;
import org.example.backend.entity.Category;
import org.example.backend.entity.Product;
import org.example.backend.entity.ProductVariant;
import org.example.backend.exception.DuplicateResourceException;
import org.example.backend.exception.InactiveProductException;
import org.example.backend.exception.InactiveVariantException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.ProductRepository;
import org.example.backend.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, ProductVariantRepository productVariantRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.isActive()) {
            throw new InactiveProductException("Category is inactive");
        }
        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .category(category)
                .active(true)
                .build();
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.isActive()) {
            throw new InactiveProductException("Category is inactive");
        }
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse setProductActive(Long id, boolean active) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(active);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductVariantResponse createVariant(Long productId, ProductVariantRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!product.isActive()) {
            throw new InactiveProductException("Product is inactive");
        }
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .weightInGrams(request.weightInGrams())
                .flavour(request.flavour())
                .eggType(request.eggType())
                .price(request.price())
                .active(true)
                .build();
        ProductVariant saved = productVariantRepository.save(variant);
        return toResponse(saved);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant does not belong to product");
        }
        variant.setWeightInGrams(request.weightInGrams());
        variant.setFlavour(request.flavour());
        variant.setEggType(request.eggType());
        variant.setPrice(request.price());
        return toResponse(productVariantRepository.save(variant));
    }

    @Transactional
    public ProductVariantResponse setVariantActive(Long productId, Long variantId, boolean active) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variant does not belong to product");
        }
        variant.setActive(active);
        return toResponse(productVariantRepository.save(variant));
    }

    public Page<ProductResponse> browseCustomerCatalog(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sort, Pageable pageable) {
        Page<Product> page = productRepository.findCustomerCatalog(search, categoryId, minPrice, maxPrice, sort, pageable);
        return page.map(this::toResponse);
    }

    public ProductResponse getActiveProduct(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toResponse(product);
    }

    public ProductVariant getProductVariantForCart(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        if (!variant.isActive()) {
            throw new InactiveVariantException("Variant is inactive");
        }
        if (!variant.getProduct().isActive()) {
            throw new InactiveProductException("Product is inactive");
        }
        if (!variant.getProduct().getCategory().isActive()) {
            throw new InactiveProductException("Category is inactive");
        }
        return variant;
    }

    public ProductVariant getProductVariantForCheckout(Long variantId) {
        return getProductVariantForCart(variantId);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.isActive(),
                productVariantRepository.findByProductId(product.getId()).stream().map(this::toResponse).toList()
        );
    }

    private ProductVariantResponse toResponse(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getWeightInGrams(),
                variant.getFlavour(),
                variant.getEggType(),
                variant.getPrice(),
                variant.isActive()
        );
    }
}
