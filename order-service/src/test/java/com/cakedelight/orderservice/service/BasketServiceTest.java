package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.dto.AddBasketItemRequest;
import com.cakedelight.orderservice.dto.BasketResponse;
import com.cakedelight.orderservice.dto.UpdateBasketItemRequest;
import com.cakedelight.orderservice.entity.Basket;
import com.cakedelight.orderservice.entity.BasketItem;
import com.cakedelight.orderservice.exception.BasketItemNotFoundException;
import com.cakedelight.orderservice.exception.CakeNotFoundException;
import com.cakedelight.orderservice.exception.CakeUnavailableException;
import com.cakedelight.orderservice.exception.CatalogUnavailableException;
import com.cakedelight.orderservice.exception.UnauthenticatedException;
import com.cakedelight.orderservice.repository.BasketRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceTest {

    @Mock
    BasketRepository basketRepository;

    @Mock
    CatalogClient catalogClient;

    @InjectMocks
    BasketService basketService;

    private static final CakeResponse AVAILABLE_CAKE =
            new CakeResponse(1L, "Chocolate Truffle", "desc", "chocolate", new BigDecimal("500.00"), true, null, null);

    @Test
    void addItem_whenUserIdHeaderMissing_throwsUnauthenticatedException() {
        AddBasketItemRequest request = new AddBasketItemRequest(1L, 2);

        assertThatThrownBy(() -> basketService.addItem(null, request))
                .isInstanceOf(UnauthenticatedException.class);

        verify(catalogClient, never()).getCake(any());
    }

    @Test
    void addItem_whenCakeNotFound_throwsCakeNotFoundException() {
        AddBasketItemRequest request = new AddBasketItemRequest(999L, 1);
        when(catalogClient.getCake(999L)).thenThrow(mock(FeignException.NotFound.class));

        assertThatThrownBy(() -> basketService.addItem("42", request))
                .isInstanceOf(CakeNotFoundException.class);

        verify(basketRepository, never()).save(any());
    }

    @Test
    void addItem_whenCakeUnavailable_throwsCakeUnavailableException() {
        AddBasketItemRequest request = new AddBasketItemRequest(1L, 1);
        CakeResponse unavailable = new CakeResponse(1L, "Mango Passion", "desc", "fruit", new BigDecimal("400.00"), false, null, null);
        when(catalogClient.getCake(1L)).thenReturn(unavailable);

        assertThatThrownBy(() -> basketService.addItem("42", request))
                .isInstanceOf(CakeUnavailableException.class);

        verify(basketRepository, never()).save(any());
    }

    @Test
    void addItem_whenCatalogUnreachable_throwsCatalogUnavailableException() {
        AddBasketItemRequest request = new AddBasketItemRequest(1L, 1);
        when(catalogClient.getCake(1L)).thenThrow(mock(FeignException.class));

        assertThatThrownBy(() -> basketService.addItem("42", request))
                .isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void addItem_whenNewCake_snapshotsNameAndPriceOntoNewItem() {
        AddBasketItemRequest request = new AddBasketItemRequest(1L, 2);
        when(catalogClient.getCake(1L)).thenReturn(AVAILABLE_CAKE);
        when(basketRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(basketRepository.save(any(Basket.class))).thenAnswer(inv -> inv.getArgument(0));

        BasketResponse result = basketService.addItem("42", request);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).cakeId()).isEqualTo(1L);
        assertThat(result.items().get(0).cakeName()).isEqualTo("Chocolate Truffle");
        assertThat(result.items().get(0).unitPrice()).isEqualByComparingTo("500.00");
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void addItem_whenCakeAlreadyInBasket_incrementsExistingQuantityInsteadOfDuplicating() {
        Basket basket = new Basket();
        basket.setId(10L);
        basket.setUserId(42L);
        BasketItem existing = new BasketItem();
        existing.setId(5L);
        existing.setBasket(basket);
        existing.setCakeId(1L);
        existing.setCakeNameSnapshot("Chocolate Truffle");
        existing.setUnitPriceSnapshot(new BigDecimal("500.00"));
        existing.setQuantity(2);
        basket.getItems().add(existing);

        AddBasketItemRequest request = new AddBasketItemRequest(1L, 3);
        when(catalogClient.getCake(1L)).thenReturn(AVAILABLE_CAKE);
        when(basketRepository.findByUserId(42L)).thenReturn(Optional.of(basket));
        when(basketRepository.save(basket)).thenReturn(basket);

        basketService.addItem("42", request);

        assertThat(basket.getItems()).hasSize(1);
        assertThat(basket.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItem_whenItemNotFound_throwsBasketItemNotFoundException() {
        when(basketRepository.findByUserId(42L)).thenReturn(Optional.empty());
        UpdateBasketItemRequest request = new UpdateBasketItemRequest(3);

        assertThatThrownBy(() -> basketService.updateItem("42", 999L, request))
                .isInstanceOf(BasketItemNotFoundException.class);
    }

    @Test
    void updateItem_whenValid_updatesQuantity() {
        Basket basket = new Basket();
        basket.setId(10L);
        BasketItem item = new BasketItem();
        item.setId(5L);
        item.setBasket(basket);
        item.setCakeId(1L);
        item.setCakeNameSnapshot("Cake");
        item.setUnitPriceSnapshot(BigDecimal.TEN);
        item.setQuantity(1);
        basket.getItems().add(item);

        when(basketRepository.findByUserId(42L)).thenReturn(Optional.of(basket));
        when(basketRepository.save(basket)).thenReturn(basket);

        basketService.updateItem("42", 5L, new UpdateBasketItemRequest(10));

        assertThat(item.getQuantity()).isEqualTo(10);
    }

    @Test
    void removeItem_whenValid_removesItemFromBasket() {
        Basket basket = new Basket();
        basket.setId(10L);
        BasketItem item = new BasketItem();
        item.setId(5L);
        item.setBasket(basket);
        basket.getItems().add(item);

        when(basketRepository.findByUserId(42L)).thenReturn(Optional.of(basket));

        basketService.removeItem("42", 5L);

        assertThat(basket.getItems()).isEmpty();
        verify(basketRepository).save(basket);
    }

    @Test
    void getBasket_whenNoBasketExistsYet_createsOne() {
        when(basketRepository.findByUserId(42L)).thenReturn(Optional.empty());
        when(basketRepository.save(any(Basket.class))).thenAnswer(inv -> inv.getArgument(0));

        BasketResponse result = basketService.getBasket("42");

        assertThat(result.userId()).isEqualTo(42L);
        verify(basketRepository).save(any(Basket.class));
    }
}
