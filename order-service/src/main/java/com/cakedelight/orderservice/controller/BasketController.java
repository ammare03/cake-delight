package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.dto.AddBasketItemRequest;
import com.cakedelight.orderservice.dto.BasketResponse;
import com.cakedelight.orderservice.dto.UpdateBasketItemRequest;
import com.cakedelight.orderservice.service.BasketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Every /orders/** route requires auth at the gateway (CLAUDE.md §4 doesn't
// list any as public) — X-User-Id is always expected to be present here.
// Parsing/validating it happens once, in BasketService (mirrors
// rating-service's RatingService pattern) rather than being duplicated here.
@RestController
@RequestMapping("/orders/basket")
@RequiredArgsConstructor
public class BasketController {

    private final BasketService basketService;

    @Operation(summary = "Get the current user's basket")
    @GetMapping
    public BasketResponse getBasket(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return basketService.getBasket(userId);
    }

    @Operation(summary = "Add a cake to the basket (or increase its quantity if already present)")
    @ApiResponse(responseCode = "201", description = "Item added")
    @ApiResponse(responseCode = "404", description = "Cake not found")
    @ApiResponse(responseCode = "409", description = "Cake is not currently available")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public BasketResponse addItem(
            @Valid @RequestBody AddBasketItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return basketService.addItem(userId, request);
    }

    @Operation(summary = "Update a basket item's quantity")
    @ApiResponse(responseCode = "200", description = "Item updated")
    @ApiResponse(responseCode = "404", description = "Basket item not found")
    @PutMapping("/items/{itemId}")
    public BasketResponse updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateBasketItemRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        return basketService.updateItem(userId, itemId, request);
    }

    @Operation(summary = "Remove an item from the basket")
    @ApiResponse(responseCode = "204", description = "Item removed")
    @ApiResponse(responseCode = "404", description = "Basket item not found")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        basketService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }
}
