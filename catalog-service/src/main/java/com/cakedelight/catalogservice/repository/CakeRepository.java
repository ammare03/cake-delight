package com.cakedelight.catalogservice.repository;

import com.cakedelight.catalogservice.entity.Cake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// JpaSpecificationExecutor, not a pile of derived findByXAndYOrZ methods —
// the catalog's filters (name/category/minPrice/maxPrice) are all optional
// and independently combinable, which is exactly what Specifications are
// for. See CakeSpecifications.
public interface CakeRepository extends JpaRepository<Cake, Long>, JpaSpecificationExecutor<Cake> {
}
