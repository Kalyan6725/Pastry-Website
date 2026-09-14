package org.example.backend.repository;

import org.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    Optional<Product> findByIdAndActiveTrue(Long id);

    @Query("""
        select p from Product p
        join fetch p.category c
        where p.active = true and c.active = true
          and (:categoryId is null or c.id = :categoryId)
          and (
              :search is null or :search = ''
              or lower(p.name) like lower(concat('%', :search, '%'))
              or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%'))
          )
          and (:minPrice is null or exists (
              select 1 from ProductVariant pv where pv.product = p and pv.active = true and pv.price >= :minPrice
          ))
          and (:maxPrice is null or exists (
              select 1 from ProductVariant pv where pv.product = p and pv.active = true and pv.price <= :maxPrice
          ))
        """)
    Page<Product> findCustomerCatalog(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
