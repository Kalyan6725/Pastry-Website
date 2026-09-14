package org.example.backend.service;

import org.example.backend.dto.payment.PaymentResponse;
import org.example.backend.entity.Order;
import org.example.backend.entity.Payment;
import org.example.backend.enums.OrderStatus;
import org.example.backend.enums.PaymentGateway;
import org.example.backend.enums.PaymentMethod;
import org.example.backend.enums.PaymentStatus;
import org.example.backend.exception.ForbiddenException;
import org.example.backend.exception.PaymentException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.OrderRepository;
import org.example.backend.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public PaymentResponse createPaymentForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.UPI)
                .gateway(PaymentGateway.RAZORPAY)
                .gatewayOrderId("RAZORPAY-" + UUID.randomUUID())
                .build();
        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    public List<PaymentResponse> getPaymentsForOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return paymentRepository.findByOrderId(order.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PaymentResponse retryPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (!payment.getOrder().getCustomer().getId().equals(userId)) {
            throw new ForbiddenException("Payment does not belong to current user");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentException("Payment already succeeded");
        }
        Payment retried = Payment.builder()
                .order(payment.getOrder())
                .amount(payment.getAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : PaymentMethod.UPI)
                .gateway(payment.getGateway())
                .gatewayOrderId(payment.getGatewayOrderId() + "-retry-" + UUID.randomUUID())
                .build();
        return toResponse(paymentRepository.save(retried));
    }

    @Transactional
    public PaymentResponse webhook(Map<String, Object> payload) {
        String gateway = String.valueOf(payload.getOrDefault("gateway", PaymentGateway.RAZORPAY.name()));
        String gatewayOrderId = String.valueOf(payload.getOrDefault("gatewayOrderId", ""));
        String status = String.valueOf(payload.getOrDefault("status", "FAILED"));

        if (gatewayOrderId.isBlank()) {
            throw new PaymentException("Gateway order id is required");
        }

        Payment payment = paymentRepository.findByGatewayAndGatewayOrderId(gateway, gatewayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for gateway order id"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        payment.setStatus("SUCCESS".equalsIgnoreCase(status) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        if (payment.getStatus() == PaymentStatus.SUCCESS && payment.getOrder().getStatus() == OrderStatus.PLACED) {
            payment.getOrder().setStatus(OrderStatus.CONFIRMED);
        }
        return toResponse(paymentRepository.save(payment));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getGateway(),
                payment.getGatewayOrderId(),
                payment.getGatewayPaymentId()
        );
    }
}
