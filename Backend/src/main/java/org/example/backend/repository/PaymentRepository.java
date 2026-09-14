package org.example.backend.repository;

import org.example.backend.entity.Payment;
import org.example.backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);
    Optional<Payment> findByGatewayAndGatewayOrderId(String gateway, String gatewayOrderId);
    Optional<Payment> findByGatewayAndGatewayPaymentId(String gateway, String gatewayPaymentId);
}
