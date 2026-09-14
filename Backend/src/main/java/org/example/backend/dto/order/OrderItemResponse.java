package org.example.backend.dto.order;

import org.example.backend.enums.EggType;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        String productName,
        Integer weightInGrams,
        String flavour,
        EggType eggType,
        Integer quantity,
        BigDecimal unitPrice
) {
}
