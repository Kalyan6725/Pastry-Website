package org.example.backend.dto.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.backend.enums.EggType;

import java.math.BigDecimal;

public record ProductVariantRequest(
        Integer weightInGrams,
        String flavour,
        EggType eggType,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        BigDecimal price
) {
}
