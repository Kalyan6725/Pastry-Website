package org.example.backend.dto.product;

public record ProductImageResponse(
        String imagePublicId,
        String imageUrl
) {
}
