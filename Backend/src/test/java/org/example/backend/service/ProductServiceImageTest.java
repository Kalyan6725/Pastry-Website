package org.example.backend.service;

import org.example.backend.dto.product.ProductRequest;
import org.example.backend.entity.Category;
import org.example.backend.entity.Product;
import org.example.backend.repository.CategoryRepository;
import org.example.backend.repository.ProductRepository;
import org.example.backend.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImageTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ImageStorageService imageStorageService;
    @InjectMocks private ProductService productService;

    @Test
    void createProductDeletesUploadedImageWhenDatabaseSaveFails() {
        Category category = Category.builder().id(1L).name("Cakes").active(true).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenThrow(new DataIntegrityViolationException("boom"));

        ProductRequest request = new ProductRequest("Chocolate Cake", "Rich", "https://res.cloudinary.com/demo/image/upload/v1/cake.jpg", "pastry-products/cake", 1L);

        assertThrows(DataIntegrityViolationException.class, () -> productService.createProduct(request));
        verify(imageStorageService).deleteProductImage("pastry-products/cake");
    }

    @Test
    void updateProductDeletesOldImageAfterReplacement() {
        Category category = Category.builder().id(1L).name("Cakes").active(true).build();
        Product product = Product.builder()
                .id(10L)
                .name("Chocolate Cake")
                .description("Rich")
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v1/old.jpg")
                .imagePublicId("pastry-products/old")
                .category(category)
                .active(true)
                .build();

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.findByProductId(10L)).thenReturn(List.of());

        ProductRequest request = new ProductRequest("Chocolate Cake Deluxe", "Even richer", "https://res.cloudinary.com/demo/image/upload/v2/new.jpg", "pastry-products/new", 1L);

        var response = productService.updateProduct(10L, request);

        assertEquals("pastry-products/new", response.imagePublicId());
        verify(imageStorageService).deleteProductImage("pastry-products/old");
    }
}
