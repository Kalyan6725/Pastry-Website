package org.example.backend.repository;

import org.example.backend.entity.DeliveryFeeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryFeeSettingRepository extends JpaRepository<DeliveryFeeSetting, Long> {
}
