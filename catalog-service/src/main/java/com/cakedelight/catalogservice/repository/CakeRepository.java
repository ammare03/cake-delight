package com.cakedelight.catalogservice.repository;

import com.cakedelight.catalogservice.entity.Cake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CakeRepository extends JpaRepository<Cake, Long> {

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
