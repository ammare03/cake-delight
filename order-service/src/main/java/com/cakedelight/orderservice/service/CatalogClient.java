package com.cakedelight.orderservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog-service", path = "/catalog/cakes")
public interface CatalogClient {

    @GetMapping("/{id}")
    CakeResponse getCake(@PathVariable("id") Long id);
}
