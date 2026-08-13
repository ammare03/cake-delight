package com.cakedelight.ratingservice.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", path = "/internal/orders")
public interface OrderClient {

    @GetMapping("/purchases")
    PurchaseCheckResponse checkPurchase(@RequestParam("userId") Long userId, @RequestParam("cakeId") Long cakeId);
}
