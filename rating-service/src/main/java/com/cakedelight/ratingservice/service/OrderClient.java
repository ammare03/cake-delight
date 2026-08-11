package com.cakedelight.ratingservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Resolved via Eureka using order-service's registered name (coding-guidelines
// §8) — hits order-service's internal-only /internal/orders/purchases
// endpoint directly, never through the gateway (see
// InternalOrderController's javadoc for why that path exists outside
// /orders/**).
@FeignClient(name = "order-service", path = "/internal/orders")
public interface OrderClient {

    @GetMapping("/purchases")
    PurchaseCheckResponse checkPurchase(@RequestParam("userId") Long userId, @RequestParam("cakeId") Long cakeId);
}
