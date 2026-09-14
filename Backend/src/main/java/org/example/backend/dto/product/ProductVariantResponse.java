package org.example.backend.dto.product;

import org.example.backend.enums.EggType;

import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        Long productId,
        Integer weightInGrams,
        String flavour,
        EggType eggType,
        BigDecimal price,
        boolean active
) {
}
