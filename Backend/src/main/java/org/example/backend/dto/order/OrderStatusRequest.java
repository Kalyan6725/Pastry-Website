package org.example.backend.dto.order;

import jakarta.validation.constraints.NotNull;
import org.example.backend.enums.OrderStatus;

public record OrderStatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {
}
