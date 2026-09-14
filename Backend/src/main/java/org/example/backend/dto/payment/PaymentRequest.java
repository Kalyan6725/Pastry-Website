package org.example.backend.dto.payment;

import jakarta.validation.constraints.NotNull;
import org.example.backend.enums.PaymentMethod;

public record PaymentRequest(
        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod
) {
}
