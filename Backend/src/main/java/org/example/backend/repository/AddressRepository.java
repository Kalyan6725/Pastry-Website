package org.example.backend.repository;

import org.example.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    Optional<Address> findByUserIdAndId(Long userId, Long id);
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    List<Address> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
