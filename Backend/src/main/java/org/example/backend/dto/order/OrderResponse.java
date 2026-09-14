package org.example.backend.dto.order;

import org.example.backend.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String deliveryAddressLine1,
        String deliveryCity,
        String deliveryPhone,
        OffsetDateTime createdAt,
        List<OrderItemResponse> items
) {
}
