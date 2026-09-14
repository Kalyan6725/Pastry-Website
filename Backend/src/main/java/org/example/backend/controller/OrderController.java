package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.order.OrderRequest;
import org.example.backend.dto.order.OrderResponse;
import org.example.backend.dto.order.OrderStatusRequest;
import org.example.backend.security.UserPrincipal;
import org.example.backend.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody OrderRequest request) {
        return orderService.createOrder(principal.getId(), request);
    }

    @GetMapping("/orders")
    public Page<OrderResponse> getOrders(@AuthenticationPrincipal UserPrincipal principal, Pageable pageable) {
        return orderService.getCustomerOrders(principal.getId(), pageable);
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOrder(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderService.getCustomerOrder(principal.getId(), id);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderService.cancelOrder(principal.getId(), id);
    }
}
