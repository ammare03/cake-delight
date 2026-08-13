package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.dto.OrderResponse;
import com.cakedelight.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create an order from the current user's basket, publish order.completed, and clear the basket")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "400", description = "Basket is empty")
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail
    ) {
        return orderService.checkout(userId, userEmail);
    }

    @Operation(summary = "List the current user's orders")
    @GetMapping
    public List<OrderResponse> listOrders(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return orderService.listOrders(userId);
    }

    @Operation(summary = "Get one of the current user's orders by id")
    @ApiResponse(responseCode = "200", description = "Order found")
    @ApiResponse(responseCode = "404", description = "Order not found")
    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return orderService.getOrder(userId, id);
    }
}
