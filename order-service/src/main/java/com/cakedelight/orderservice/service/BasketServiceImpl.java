package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.client.CatalogClient;
import com.cakedelight.orderservice.client.dto.CakeResponse;
import com.cakedelight.orderservice.dto.request.AddBasketItemRequest;
import com.cakedelight.orderservice.dto.request.UpdateBasketItemRequest;
import com.cakedelight.orderservice.dto.response.BasketResponse;
import com.cakedelight.orderservice.entity.Basket;
import com.cakedelight.orderservice.entity.BasketItem;
import com.cakedelight.orderservice.exception.BasketItemNotFoundException;
import com.cakedelight.orderservice.exception.CakeNotFoundException;
import com.cakedelight.orderservice.exception.CakeUnavailableException;
import com.cakedelight.orderservice.exception.CatalogUnavailableException;
import com.cakedelight.orderservice.exception.UnauthenticatedException;
import com.cakedelight.orderservice.mapper.BasketMapper;
import com.cakedelight.orderservice.repository.BasketRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasketServiceImpl implements BasketService {

    private final BasketRepository basketRepository;
    private final CatalogClient catalogClient;
    private final BasketMapper basketMapper;

    @Override
    @Transactional
    public BasketResponse getBasket(String rawUserId) {
        return basketMapper.toResponse(getOrCreateBasket(parseUserId(rawUserId)));
    }

    @Override
    @Transactional
    public BasketResponse addItem(String rawUserId, AddBasketItemRequest request) {
        Long userId = parseUserId(rawUserId);
        CakeResponse cake = fetchAvailableCake(request.cakeId());
        Basket basket = getOrCreateBasket(userId);

        // Adding a cake already in the basket increases its quantity rather
        // than creating a second line for the same cake — the natural
        // "add to cart" behavior, and keeps (basket, cake) effectively unique
        // without needing a DB constraint to enforce it.
        BasketItem item = basket.getItems().stream()
                .filter(existing -> existing.getCakeId().equals(request.cakeId()))
                .findFirst()
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity() + request.quantity());
        } else {
            item = new BasketItem();
            item.setBasket(basket);
            item.setCakeId(cake.id());
            item.setCakeNameSnapshot(cake.name());
            item.setUnitPriceSnapshot(cake.price());
            item.setQuantity(request.quantity());
            basket.getItems().add(item);
        }

        Basket saved = basketRepository.save(basket);
        log.info("User {} added cake {} x{} to basket", userId, request.cakeId(), request.quantity());
        return basketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BasketResponse updateItem(String rawUserId, Long itemId, UpdateBasketItemRequest request) {
        BasketItem item = findItemOrThrow(parseUserId(rawUserId), itemId);
        item.setQuantity(request.quantity());
        Basket saved = basketRepository.save(item.getBasket());
        log.info("Set quantity of basket item {} to {}", itemId, request.quantity());
        return basketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeItem(String rawUserId, Long itemId) {
        BasketItem item = findItemOrThrow(parseUserId(rawUserId), itemId);
        item.getBasket().getItems().remove(item); // orphanRemoval deletes the row
        basketRepository.save(item.getBasket());
        log.info("Removed basket item {}", itemId);
    }

    private Long parseUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new UnauthenticatedException();
        }
        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ex) {
            throw new UnauthenticatedException();
        }
    }

    private Basket getOrCreateBasket(Long userId) {
        return basketRepository.findByUserId(userId).orElseGet(() -> {
            Basket basket = new Basket();
            basket.setUserId(userId);
            return basketRepository.save(basket);
        });
    }

    private BasketItem findItemOrThrow(Long userId, Long itemId) {
        Basket basket = basketRepository.findByUserId(userId)
                .orElseThrow(() -> new BasketItemNotFoundException(itemId));
        return basket.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BasketItemNotFoundException(itemId));
    }

    // Resolves the cake through catalog-service and enforces the same
    // "unavailable cakes are excluded" rule catalog-service's own browse
    // endpoint applies (M1, Phase 3 audit) — a cake can still be fetched
    // directly by id even when unavailable, so this has to be checked here
    // too, not assumed from the browse contract.
    private CakeResponse fetchAvailableCake(Long cakeId) {
        CakeResponse cake;
        try {
            cake = catalogClient.getCake(cakeId);
        } catch (FeignException.NotFound ex) {
            throw new CakeNotFoundException(cakeId);
        } catch (FeignException ex) {
            log.error("catalog-service call failed for cake {}", cakeId, ex);
            throw new CatalogUnavailableException();
        }
        if (!Boolean.TRUE.equals(cake.available())) {
            throw new CakeUnavailableException(cakeId);
        }
        return cake;
    }
}
