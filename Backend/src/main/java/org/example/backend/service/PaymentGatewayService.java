package org.example.backend.service;

import org.example.backend.entity.Payment;

public interface PaymentGatewayService {
    Payment initiate(Payment payment);
    Payment verify(Payment payment);
}
