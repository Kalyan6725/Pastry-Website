package org.example.backend.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.example.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Product> findCustomerCatalog(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, Pageable pageable) {
        StringBuilder jpql = new StringBuilder("select p from Product p join p.category c ");
        StringBuilder countQuery = new StringBuilder("select count(p.id) from Product p join p.category c ");
        Map<String, Object> params = new HashMap<>();

        jpql.append("where p.active = true and c.active = true ");
        countQuery.append("where p.active = true and c.active = true ");

        if (search != null && !search.isBlank()) {
            String keyword = "%" + search.trim().toLowerCase() + "%";
            jpql.append("and (lower(p.name) like :search or lower(coalesce(p.description, '')) like :search) ");
            countQuery.append("and (lower(p.name) like :search or lower(coalesce(p.description, '')) like :search) ");
            params.put("search", keyword);
        }

        if (categoryId != null) {
            jpql.append("and c.id = :categoryId ");
            countQuery.append("and c.id = :categoryId ");
            params.put("categoryId", categoryId);
        }

        if (minPrice != null) {
            jpql.append("and exists (select 1 from ProductVariant pv where pv.product = p and pv.active = true and pv.price >= :minPrice) ");
            countQuery.append("and exists (select 1 from ProductVariant pv where pv.product = p and pv.active = true and pv.price >= :minPrice) ");
            params.put("minPrice", minPrice);
        }

        if (maxPrice != null) {
            jpql.append("and exists (select 1 from ProductVariant pv where pv.product = p and pv.active = true and pv.price <= :maxPrice) ");
            countQuery.append("and exists (select 1 from ProductVariant pv where pv.product = p and pv.active = true and pv.price <= :maxPrice) ");
            params.put("maxPrice", maxPrice);
        }

        jpql.append(buildOrderBy(sortBy));

        TypedQuery<Product> query = entityManager.createQuery(jpql.toString(), Product.class);
        params.forEach(query::setParameter);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Product> content = query.getResultList();

        TypedQuery<Long> count = entityManager.createQuery(countQuery.toString(), Long.class);
        params.forEach(count::setParameter);
        Long total = count.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private String buildOrderBy(String sortBy) {
        String normalized = sortBy == null ? "newest" : sortBy.toLowerCase();
        return switch (normalized) {
            case "name_asc" -> "order by lower(p.name) asc, p.id asc";
            case "name_desc" -> "order by lower(p.name) desc, p.id desc";
            case "price_asc" -> "order by p.id asc";
            case "price_desc" -> "order by p.id desc";
            case "newest" -> "order by p.createdAt desc, p.id desc";
            default -> "order by p.createdAt desc, p.id desc";
        };
    }
}
