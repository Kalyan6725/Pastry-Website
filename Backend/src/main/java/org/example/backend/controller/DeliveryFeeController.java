package org.example.backend.controller;

import jakarta.validation.Valid;
import org.example.backend.dto.delivery.DeliveryFeeRequest;
import org.example.backend.dto.delivery.DeliveryFeeResponse;
import org.example.backend.service.DeliveryFeeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeliveryFeeController {

    private final DeliveryFeeService deliveryFeeService;

    public DeliveryFeeController(DeliveryFeeService deliveryFeeService) {
        this.deliveryFeeService = deliveryFeeService;
    }

    @GetMapping("/delivery-fee")
    public DeliveryFeeResponse getDeliveryFee() {
        return deliveryFeeService.getFee();
    }

    @PutMapping("/admin/delivery-fee")
    public DeliveryFeeResponse updateDeliveryFee(@Valid @RequestBody DeliveryFeeRequest request) {
        return deliveryFeeService.updateFee(request);
    }
}
