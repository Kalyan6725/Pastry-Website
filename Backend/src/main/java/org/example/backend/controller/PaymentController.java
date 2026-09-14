package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.payment.PaymentRequest;
import org.example.backend.dto.payment.PaymentResponse;
import org.example.backend.security.UserPrincipal;
import org.example.backend.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId, @Valid @RequestBody PaymentRequest request) {
        return paymentService.createPaymentForOrder(orderId);
    }

    @PostMapping("/payments/{paymentId}/retry")
    public PaymentResponse retryPayment(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long paymentId) {
        return paymentService.retryPayment(principal.getId(), paymentId);
    }

    @PostMapping("/payments/webhook")
    public PaymentResponse webhook(@RequestBody Map<String, Object> payload) {
        return paymentService.webhook(payload);
    }

    @GetMapping("/orders/{orderId}/payments")
    public List<PaymentResponse> getPayments(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return paymentService.getPaymentsForOrder(principal.getId(), orderId);
    }
}
