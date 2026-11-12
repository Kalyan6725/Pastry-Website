package org.example.backend.dto.delivery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record DeliveryFeeRequest(
        @NotNull(message = "Delivery fee is required.")
        @PositiveOrZero(message = "Delivery fee cannot be negative.")
        BigDecimal amount
) {
}
