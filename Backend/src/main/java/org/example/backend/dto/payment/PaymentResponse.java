package org.example.backend.dto.payment;

import org.example.backend.enums.PaymentGateway;
import org.example.backend.enums.PaymentMethod;
import org.example.backend.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        PaymentGateway gateway,
        String gatewayOrderId,
        String gatewayPaymentId
) {
}
