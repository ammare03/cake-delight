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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketServiceImplTest {

    @Mock
    BasketRepository basketRepository;

    @Mock
    CatalogClient catalogClient;

    @Mock
    BasketMapper basketMapper;

    @InjectMocks
    BasketServiceImpl basketService;

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
        when(basketMapper.toResponse(any(Basket.class))).thenReturn(
                new BasketResponse(1L, 42L, java.util.List.of(), BigDecimal.ZERO));

        basketService.addItem("42", request);

        ArgumentCaptor<Basket> captor = ArgumentCaptor.forClass(Basket.class);
        verify(basketMapper).toResponse(captor.capture());
        Basket saved = captor.getValue();
        assertThat(saved.getItems()).hasSize(1);
        BasketItem item = saved.getItems().get(0);
        assertThat(item.getCakeId()).isEqualTo(1L);
        assertThat(item.getCakeNameSnapshot()).isEqualTo("Chocolate Truffle");
        assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("500.00");
        assertThat(item.getQuantity()).isEqualTo(2);
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
        when(basketMapper.toResponse(basket)).thenReturn(
                new BasketResponse(10L, 42L, java.util.List.of(), BigDecimal.ZERO));

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
        item.setQuantity(1);
        basket.getItems().add(item);

        when(basketRepository.findByUserId(42L)).thenReturn(Optional.of(basket));
        when(basketRepository.save(basket)).thenReturn(basket);
        when(basketMapper.toResponse(basket)).thenReturn(
                new BasketResponse(10L, 42L, java.util.List.of(), BigDecimal.ZERO));

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
        when(basketMapper.toResponse(any(Basket.class))).thenReturn(
                new BasketResponse(1L, 42L, java.util.List.of(), BigDecimal.ZERO));

        BasketResponse result = basketService.getBasket("42");

        assertThat(result.userId()).isEqualTo(42L);
        verify(basketRepository).save(any(Basket.class));
    }
}
