package org.example.backend.dto.order;

import jakarta.validation.constraints.NotNull;
import org.example.backend.enums.PaymentMethod;

public record OrderRequest(
        @NotNull(message = "Address id is required")
        Long addressId,
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {
}
