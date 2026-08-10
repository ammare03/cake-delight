package com.cakedelight.catalogservice.repository;

import com.cakedelight.catalogservice.entity.Cake;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

/** Small composable filters for GET /catalog/cakes — each is a no-op when its argument is null. */
public final class CakeSpecifications {

    private CakeSpecifications() {
    }

    public static Specification<Cake> hasName(String name) {
        return (root, query, cb) -> name == null ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Cake> hasCategory(String category) {
        return (root, query, cb) -> category == null ? null
                : cb.equal(cb.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<Cake> minPrice(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null
                : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Cake> maxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null
                : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    /** Unconditional, not a filter param — browse never shows unavailable cakes (CLAUDE.md §5.2, SD-C1). */
    public static Specification<Cake> isAvailable() {
        return (root, query, cb) -> cb.isTrue(root.get("available"));
    }
}
