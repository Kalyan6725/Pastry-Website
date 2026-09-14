package org.example.backend.repository;

import org.example.backend.entity.Order;
import org.example.backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Optional<Order> findById(Long id);
    boolean existsByIdAndCustomerId(Long id, Long customerId);
    Page<Order> findAll(Pageable pageable);
    long countByStatus(OrderStatus status);
}
