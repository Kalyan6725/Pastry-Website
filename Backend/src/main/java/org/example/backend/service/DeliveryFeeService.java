package org.example.backend.service;

import org.example.backend.dto.delivery.DeliveryFeeRequest;
import org.example.backend.dto.delivery.DeliveryFeeResponse;
import org.example.backend.entity.DeliveryFeeSetting;
import org.example.backend.repository.DeliveryFeeSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryFeeService {

    private final DeliveryFeeSettingRepository repository;

    public DeliveryFeeService(DeliveryFeeSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DeliveryFeeResponse getFee() {
        return new DeliveryFeeResponse(getOrCreate().getAmount());
    }

    @Transactional
    public DeliveryFeeResponse updateFee(DeliveryFeeRequest request) {
        DeliveryFeeSetting setting = getOrCreate();
        setting.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        return new DeliveryFeeResponse(repository.save(setting).getAmount());
    }

    public BigDecimal currentAmount() {
        return getOrCreate().getAmount();
    }

    private DeliveryFeeSetting getOrCreate() {
        return repository.findById(DeliveryFeeSetting.DEFAULT_ID)
                .orElseGet(() -> repository.save(DeliveryFeeSetting.builder()
                        .id(DeliveryFeeSetting.DEFAULT_ID)
                        .amount(DeliveryFeeSetting.DEFAULT_FEE)
                        .build()));
    }
}
