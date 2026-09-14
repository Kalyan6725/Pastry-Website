package org.example.backend.dto.product;

import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String imageUrl,
        Long categoryId,
        String categoryName,
        boolean active,
        List<ProductVariantResponse> variants
) {
}
