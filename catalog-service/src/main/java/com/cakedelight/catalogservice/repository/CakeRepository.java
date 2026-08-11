package com.cakedelight.catalogservice.repository;

import com.cakedelight.catalogservice.entity.Cake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CakeRepository extends JpaRepository<Cake, Long> {

    // One query for all four optional filters (name/category/minPrice/maxPrice)
    // — a null parameter just makes its own condition always true, so browsing
    // with no filters and browsing with all four filters go through the same
    // query. Available is unconditional, not one of the filters (SD-C1: browse
    // never shows unavailable cakes).
    @Query("""
            SELECT c FROM Cake c
            WHERE c.available = true
              AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:category IS NULL OR LOWER(c.category) = LOWER(:category))
              AND (:minPrice IS NULL OR c.price >= :minPrice)
              AND (:maxPrice IS NULL OR c.price <= :maxPrice)
            """)
    List<Cake> search(@Param("name") String name,
                       @Param("category") String category,
                       @Param("minPrice") BigDecimal minPrice,
                       @Param("maxPrice") BigDecimal maxPrice);
}
