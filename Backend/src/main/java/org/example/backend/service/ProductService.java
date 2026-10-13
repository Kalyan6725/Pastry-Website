package org.example.backend.service;

import org.example.backend.dto.product.ProductRequest;
import org.example.backend.dto.product.ProductResponse;
import org.example.backend.dto.product.ProductVariantRequest;
import org.example.backend.dto.product.ProductVariantResponse;
import org.example.backend.dto.product.ProductImageResponse;
import org.example.backend.entity.Category;
import org.example.backend.entity.Product;
import org.example.backend.entity.ProductVariant;
import org.example.backend.exception.BadRequestException;
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
    private final ImageStorageService imageStorageService;

    public ProductService(ProductRepository productRepository, ProductVariantRepository productVariantRepository, CategoryRepository categoryRepository, ImageStorageService imageStorageService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.isActive()) {
            throw new InactiveProductException("Category is inactive");
        }

        String imageUrl = normalize(request.imageUrl());
        String imagePublicId = normalize(request.imagePublicId());
        requireImage(imageUrl, imagePublicId, true);

        Product product = Product.builder()
                .name(request.name().trim())
                .description(request.description())
                .imagePublicId(imagePublicId)
                .imageUrl(imageUrl)
                .category(category)
                .active(true)
                .build();
        try {
            Product saved = productRepository.save(product);
            return toResponse(saved);
        } catch (RuntimeException ex) {
            imageStorageService.deleteProductImage(imagePublicId);
            throw ex;
        }
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
        String previousImagePublicId = normalize(product.getImagePublicId());
        String previousImageUrl = normalize(product.getImageUrl());
        String imageUrl = normalize(request.imageUrl());
        String imagePublicId = normalize(request.imagePublicId());
        boolean legacyImage = previousImagePublicId == null && imagePublicId == null && imageUrl != null;
        if ((imageUrl != null || imagePublicId != null)
                && !legacyImage
                && (imageUrl == null || imagePublicId == null)) {
            throw new BadRequestException("Image upload must include both the Cloudinary URL and public ID.");
        }
        if (imageUrl != null) {
            product.setImageUrl(imageUrl);
        }
        if (imagePublicId != null) {
            product.setImagePublicId(imagePublicId);
        }
        product.setCategory(category);
        try {
            Product saved = productRepository.save(product);
            if (previousImagePublicId != null && imagePublicId != null && !previousImagePublicId.equals(imagePublicId)) {
                imageStorageService.deleteProductImage(previousImagePublicId);
            }
            return toResponse(saved);
        } catch (RuntimeException ex) {
            if (imagePublicId != null && !imagePublicId.equals(previousImagePublicId)) {
                imageStorageService.deleteProductImage(imagePublicId);
            }
            if (imageUrl == null && previousImageUrl != null) {
                product.setImageUrl(previousImageUrl);
            }
            if (imagePublicId == null && previousImagePublicId != null) {
                product.setImagePublicId(previousImagePublicId);
            }
            throw ex;
        }
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

    public ProductImageResponse uploadProductImage(org.springframework.web.multipart.MultipartFile file) {
        return imageStorageService.uploadProductImage(file);
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
                product.getImagePublicId(),
                product.getImageUrl(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.isActive(),
                productVariantRepository.findByProductId(product.getId()).stream().map(this::toResponse).toList()
        );
    }

    private void requireImage(String imageUrl, String imagePublicId, boolean creating) {
        boolean hasUrl = imageUrl != null;
        boolean hasPublicId = imagePublicId != null;
        if (hasUrl != hasPublicId) {
            throw new BadRequestException("Image upload must include both the Cloudinary URL and public ID.");
        }
        if (creating && (!hasUrl || !hasPublicId)) {
            throw new BadRequestException("Upload a product image before saving the product.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
