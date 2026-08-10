package com.cakedelight.orderservice.client;

import com.cakedelight.orderservice.client.dto.CakeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Resolved via Eureka using catalog-service's registered name — no hardcoded
// URL (coding-guidelines §8). Used at basket-add time to resolve the cake's
// current name/price/availability before it's snapshotted onto a BasketItem.
@FeignClient(name = "catalog-service", path = "/catalog/cakes")
public interface CatalogClient {

    @GetMapping("/{id}")
    CakeResponse getCake(@PathVariable("id") Long id);
}
