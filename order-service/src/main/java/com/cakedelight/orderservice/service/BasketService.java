package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.dto.request.AddBasketItemRequest;
import com.cakedelight.orderservice.dto.request.UpdateBasketItemRequest;
import com.cakedelight.orderservice.dto.response.BasketResponse;

// Every method takes the raw X-User-Id header value, not a parsed Long — see
// rating-service's RatingService for the same pattern. Parsing (and throwing
// UnauthenticatedException on a missing/malformed header) happens once, in
// the Impl, rather than being duplicated across every controller method.
public interface BasketService {

    BasketResponse getBasket(String rawUserId);

    BasketResponse addItem(String rawUserId, AddBasketItemRequest request);

    BasketResponse updateItem(String rawUserId, Long itemId, UpdateBasketItemRequest request);

    void removeItem(String rawUserId, Long itemId);
}
