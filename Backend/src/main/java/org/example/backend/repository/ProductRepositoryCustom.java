package org.example.backend.repository;

import org.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductRepositoryCustom {
    Page<Product> findCustomerCatalog(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, Pageable pageable);
}
