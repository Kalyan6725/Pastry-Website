package org.example.backend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Product name must be at most 200 characters")
        String name,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotBlank(message = "Image URL is required")
        @Size(max = 1000, message = "Image URL must be at most 1000 characters")
        String imageUrl,

        @NotNull(message = "Category is required")
        Long categoryId
) {
}
