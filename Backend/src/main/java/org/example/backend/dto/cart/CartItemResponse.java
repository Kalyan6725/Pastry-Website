package org.example.backend.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productVariantId,
        String productName,
        String variantLabel,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
