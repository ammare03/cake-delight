package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.dto.PurchaseCheckResponse;
import com.cakedelight.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @Operation(summary = "Internal: has this user purchased this cake? (used by rating-service)")
    @GetMapping("/purchases")
    public PurchaseCheckResponse checkPurchase(@RequestParam Long userId, @RequestParam Long cakeId) {
        return new PurchaseCheckResponse(orderService.hasPurchased(userId, cakeId));
    }
}
