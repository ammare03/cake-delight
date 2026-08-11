package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.dto.PurchaseCheckResponse;
import com.cakedelight.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only — called by rating-service via Feign
 * (lb://order-service, resolved through Eureka, never through the gateway)
 * to back the purchase-verification rule CLAUDE.md §5.2 asks for ("only
 * users who purchased the cake can rate it").
 *
 * Deliberately mounted at /internal/orders, not under /orders/** — the
 * gateway's existing route (config-repo/api-gateway.properties,
 * Path=/api/orders/**) would otherwise proxy this too, letting any
 * authenticated end user query whether an arbitrary *other* user purchased
 * an arbitrary cake, which is a real authorization leak (this endpoint takes
 * userId as a plain parameter, not from a trusted X-User-Id header, since
 * its caller is another service, not an end user through the gateway).
 * There is no gateway route for /api/internal/**, so this path 404s at the
 * gateway rather than needing extra deny-list logic to stay unreachable
 * from outside the cluster.
 */
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
